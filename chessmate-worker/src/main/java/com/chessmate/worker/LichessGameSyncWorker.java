package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.Game;
import com.chessmate.domain.game.GameRepository;
import com.chessmate.domain.game.GameResult;
import com.chessmate.domain.sync.SyncJob;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.external.api.lichess.LichessApi;
import com.chessmate.external.dto.game.LichessGamesDto;
import com.chessmate.external.dto.game.Player;
import com.chessmate.infra_redis.token.PlatformTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Lichess 게임 수집 Worker.
 *
 * [전체 수집 - 최초 로그인]
 * - DB에 게임이 없을 때: until=gameId 역방향 커서 페이징 (dateDesc)
 * - RateLimiter: 0.05 permits/sec = 분당 3회 (100개 × 3 = 300게임/분)
 *
 * [증분 수집 - 정기 스케쥴러]
 * - DB에 게임이 있을 때: since=epochMillis 순방향 페이징 (dateAsc)
 * - 신규 게임만 API 요청 → DB saveAll 중복 체크로 이중 방어
 * - 신규 게임이 있을 때만 stat 재집계
 *
 * [토큰 없는 유저 (직접 INSERT된 유저)]
 * - OAuth 토큰 없이 Lichess 공개 API로 수집 진행
 * - RateLimiter(0.05 req/s)가 비인증 한도(20 req/min) 이내이므로 안전
 * - 이후 OAuth 로그인 시 다음 스케줄부터 자동으로 인증 모드 전환
 *
 * Lichess API (2025-04): https://lichess.org/api#tag/Games/operation/apiGamesUser
 * Rate Limit: ~20 req/s (OAuth 인증), 비인증 20 req/min
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LichessGameSyncWorker {

    private static final int CHUNK_SIZE = 100;
    private static final String NDJSON = "application/x-ndjson";
    private static final Set<String> SUPPORTED_TIME_CLASSES = Set.of("blitz", "bullet", "rapid", "classical");

    @SuppressWarnings("UnstableApiUsage")
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(0.05);

    private final LichessApi lichessApi;
    private final PlatformTokenStore tokenStore;
    private final SyncJobRepository syncJobRepository;
    private final GameRepository gameRepository;
    private final GameStatAggregator statAggregator;
    private final PerfStatFetcher perfStatFetcher;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("UnstableApiUsage")
    public void process(SyncJob job) {
        Long userId = job.getUserId();
        String username = job.getPlatformUsername();

        try {
            String accessToken = tokenStore.getLichessAccessToken(userId);
            if (accessToken == null) {
                log.info("[LichessWorker] 토큰 없음 — 공개 API로 수집 진행 userId={}", userId);
            }

            job.start();
            syncJobRepository.save(job);

            // DB에 게임이 있으면 증분 수집, 없으면 전체 수집
            Optional<LocalDateTime> latestPlayedAt =
                gameRepository.findLatestPlayedAtByUserIdAndPlatform(userId, OAuthPlatForm.LICHESS);

            if (latestPlayedAt.isPresent()) {
                log.info("[LichessWorker] 증분 수집 시작 userId={} username={} since={}", userId, username, latestPlayedAt.get());
                processIncremental(job, userId, username, latestPlayedAt.get(), accessToken);
            } else {
                log.info("[LichessWorker] 전체 수집 시작 userId={} username={}", userId, username);
                processFullSync(job, userId, username, accessToken);
            }
        } catch (Exception e) {
            log.error("[LichessWorker] 수집 실패 userId={} error={}", userId, e.getMessage(), e);
            job.fail(e.getMessage());
            syncJobRepository.save(job);
        }
    }

    /** 전체 수집: until(gameId) 역방향 커서 페이징 */
    @SuppressWarnings("UnstableApiUsage")
    private void processFullSync(SyncJob job, Long userId, String username, String accessToken) {
        try {
            String untilGameId = job.getSyncCursor();

            while (true) {
                RATE_LIMITER.acquire();

                String ndjson = lichessApi.getGames(
                    buildAuthHeader(accessToken), NDJSON, username,
                    CHUNK_SIZE, untilGameId, null, "dateDesc", null
                );

                List<LichessGamesDto> games = parseNdjson(ndjson);
                if (games.isEmpty()) break;

                List<Game> toSave = games.stream()
                    .filter(g -> g.rated())
                    .filter(g -> SUPPORTED_TIME_CLASSES.contains(g.perf()))
                    .filter(g -> g.createdAt() != null && g.createdAt() > 0)
                    .map(g -> toGame(userId, username, g))
                    .toList();

                List<Game> saved = gameRepository.saveAll(toSave);
                log.info("[LichessWorker] 전체수집 청크 {}개 → {}개 저장", games.size(), saved.size());

                untilGameId = games.get(games.size() - 1).id();
                job.progress(untilGameId, saved.size());
                syncJobRepository.save(job);

                if (games.size() < CHUNK_SIZE) break;
            }

            job.complete();
            syncJobRepository.save(job);
            log.info("[LichessWorker] 전체 수집 완료 userId={} total={}", userId, job.getTotalFetched());

            statAggregator.aggregate(userId, OAuthPlatForm.LICHESS);
            perfStatFetcher.fetch(userId, OAuthPlatForm.LICHESS, username);

        } catch (Exception e) {
            log.error("[LichessWorker] 전체 수집 실패 userId={} error={}", userId, e.getMessage(), e);
            job.fail(e.getMessage());
            syncJobRepository.save(job);
        }
    }

    /**
     * 증분 수집: since(epochMillis) 순방향 페이징.
     *
     * - since = latestPlayedAt + 1ms → 마지막으로 저장된 게임 직후부터 수집
     * - dateAsc: 오래된 것부터 → 다음 since를 마지막 게임 timestamp로 갱신
     * - 신규 게임이 없으면 stat 재집계 생략 (중복 집계 방지 + 불필요한 연산 제거)
     *
     * Chess.com과 달리 Lichess는 since 파라미터를 API 레벨에서 지원하므로
     * 불필요한 API 호출 자체가 발생하지 않음.
     */
    @SuppressWarnings("UnstableApiUsage")
    private void processIncremental(SyncJob job, Long userId, String username, LocalDateTime latestPlayedAt, String accessToken) {
        try {
            // +1ms: 마지막 게임과 정확히 같은 시간대 게임 재수집 방지
            long since = latestPlayedAt.toInstant(ZoneOffset.UTC).toEpochMilli() + 1L;
            int totalNewlySaved = 0;

            while (true) {
                RATE_LIMITER.acquire();

                String ndjson = lichessApi.getGames(
                    buildAuthHeader(accessToken), NDJSON, username,
                    CHUNK_SIZE, null, since, "dateAsc", null
                );

                List<LichessGamesDto> games = parseNdjson(ndjson);
                if (games.isEmpty()) break;

                List<Game> toSave = games.stream()
                    .filter(g -> g.rated())
                    .filter(g -> SUPPORTED_TIME_CLASSES.contains(g.perf()))
                    .filter(g -> g.createdAt() != null && g.createdAt() > 0)
                    .map(g -> toGame(userId, username, g))
                    .toList();

                // saveAll 내부에서 platformGameId 중복 제거 → 실제 신규만 저장
                List<Game> saved = gameRepository.saveAll(toSave);
                totalNewlySaved += saved.size();
                log.info("[LichessWorker] 증분 청크 {}개 → {}개 신규 저장", games.size(), saved.size());

                // 다음 페이지: 마지막 게임 시간 +1ms를 새 since로 사용
                LichessGamesDto last = games.getLast();
                since = last.createdAt() + 1L;

                job.progress(last.id(), saved.size());
                syncJobRepository.save(job);

                if (games.size() < CHUNK_SIZE) break;
            }

            job.complete();
            syncJobRepository.save(job);
            log.info("[LichessWorker] 증분 수집 완료 userId={} 신규={}건", userId, totalNewlySaved);

            // 신규 게임이 저장됐거나, stat 테이블 중 하나라도 비어 있으면 재집계
            if (totalNewlySaved > 0 || statAggregator.isAnyStatEmpty(userId, OAuthPlatForm.LICHESS)) {
                statAggregator.aggregate(userId, OAuthPlatForm.LICHESS);
                perfStatFetcher.fetch(userId, OAuthPlatForm.LICHESS, username);
            }

        } catch (Exception e) {
            log.error("[LichessWorker] 증분 수집 실패 userId={} error={}", userId, e.getMessage(), e);
            job.fail(e.getMessage());
            syncJobRepository.save(job);
        }
    }

    /** token이 있으면 "Bearer {token}", 없으면 null (헤더 생략 → 공개 API) */
    private String buildAuthHeader(String token) {
        return token != null ? "Bearer " + token : null;
    }

    private Game toGame(Long userId, String username, LichessGamesDto dto) {
        boolean isWhite = isWhitePlayer(dto, username);
        String playerColor = isWhite ? "WHITE" : "BLACK";
        String opponentUsername = resolveOpponentUsername(dto, isWhite);
        GameResult result = resolveResult(dto.winner(), playerColor);
        Integer rating = resolveRating(dto, isWhite);

        return Game.builder()
            .userId(userId)
            .platform(OAuthPlatForm.LICHESS)
            .platformGameId(dto.id())
            .username(username)
            .opponentUsername(opponentUsername)
            .playerColor(playerColor)
            .result(result)
            .timeClass(dto.perf())
            .rated(dto.rated())
            .rating(rating)
            .moves(dto.moves())
            .variant(dto.variant())
            .playedAt(epochMillisToLocalDateTime(dto.createdAt()))
            .createdAt(LocalDateTime.now())
            .build();
    }

    private Integer resolveRating(LichessGamesDto dto, boolean isWhite) {
        if (dto.players() == null) return null;
        var player = isWhite ? dto.players().white() : dto.players().black();
        if (player == null || player.rating() == null) return null;
        // rated 게임은 rating + ratingDiff = 게임 후 실제 레이팅
        // casual 게임은 ratingDiff가 null이므로 entry rating 그대로
        int diff = player.ratingDiff() != null ? player.ratingDiff() : 0;
        return player.rating() + diff;
    }

    private boolean isWhitePlayer(LichessGamesDto dto, String username) {
        if (dto.players() == null || dto.players().white() == null) return false;
        Player white = dto.players().white();
        return white.user() != null && username.equalsIgnoreCase(white.user().name());
    }

    private String resolveOpponentUsername(LichessGamesDto dto, boolean isWhite) {
        if (dto.players() == null) return null;
        Player opponent = isWhite ? dto.players().black() : dto.players().white();
        if (opponent == null || opponent.user() == null) return null;
        return opponent.user().name();
    }

    private GameResult resolveResult(String winner, String playerColor) {
        if (winner == null) return GameResult.DRAW;
        boolean playerIsWhite = "WHITE".equals(playerColor);
        boolean whiteWon = "white".equals(winner);
        return (playerIsWhite == whiteWon) ? GameResult.WIN : GameResult.LOSS;
    }

    private LocalDateTime epochMillisToLocalDateTime(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }

    private List<LichessGamesDto> parseNdjson(String ndjson) {
        List<LichessGamesDto> result = new ArrayList<>();
        if (ndjson == null || ndjson.isBlank()) return result;

        for (String line : ndjson.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                result.add(objectMapper.readValue(line, LichessGamesDto.class));
            } catch (Exception e) {
                log.warn("[LichessWorker] NDJSON 파싱 실패: {}", line);
            }
        }
        return result;
    }
}
