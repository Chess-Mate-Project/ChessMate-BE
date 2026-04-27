package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.Game;
import com.chessmate.domain.game.GameRepository;
import com.chessmate.domain.game.GameResult;
import com.chessmate.domain.sync.SyncJob;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.external.api.chesscom.ChesscomApi;
import com.chessmate.external.dto.ChesscomMonthlyArchiveResponse;
import com.chessmate.external.dto.chesscom.ChesscomGameArchivesResponse;
import com.chessmate.external.dto.chesscom.ChesscomGamePlayerInfo;
import com.chessmate.external.dto.chesscom.ChesscomGameResponse;
import com.chessmate.infra_redis.token.PlatformTokenStore;
import com.google.common.util.concurrent.RateLimiter;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Chess.com 게임 수집 Worker.
 *
 * [전체 수집 - 최초 로그인]
 * - cursor=null: 전체 아카이브 수집
 *
 * [증분 수집 - 정기 스케쥴러]
 * - ScheduledSyncTrigger가 cursor를 전월(yyyy/MM)으로 사전 설정
 * - filterPending이 당월 아카이브만 반환 → 당월 게임만 재수집
 * - saveAll 내부 중복 체크로 이미 저장된 게임 자동 스킵 → 중복 집계 방지
 * - 신규 게임이 실제로 저장됐을 때만 stat 재집계
 *
 * Chess.com Public API (2025-04): https://www.chess.com/news/view/published-data-api
 * - 월별 아카이브: GET /pub/player/{user}/games/{year}/{month}
 * - 인증 없이도 공개 데이터 접근 가능 (OAuth 토큰은 미래 기능 대비)
 * - Rate Limit: 공식 명시 없음, 실제 운용 기준 1~2 req/s 권장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChessComGameSyncWorker {

    private static final int  MAX_RETRIES         = 5;
    private static final long BASE_BACKOFF_MS      = 60_000L;
    private static final long BACKOFF_INCREMENT_MS = 5_000L;

    @Value("${chessmate.sync.chesscom.parallelism:5}")
    private int parallelism;

    @SuppressWarnings("UnstableApiUsage")
    private static final RateLimiter FALLBACK_RATE_LIMITER = RateLimiter.create(0.5);

    // 토큰 있는 병렬 모드에서도 RateLimiter 적용 (429 방지)
    @SuppressWarnings("UnstableApiUsage")
    private static final RateLimiter PARALLEL_RATE_LIMITER = RateLimiter.create(2.0);

    private static final Set<String> SUPPORTED_TIME_CLASSES = Set.of("blitz", "bullet", "rapid", "daily");

    // Chess.com result 값 → GameResult 매핑
    private static final Set<String> DRAW_RESULTS = Set.of(
        "agreed", "repetition", "stalemate", "insufficient", "50move", "timevsinsufficient", "kingofthehill"
    );

    private final ChesscomApi chesscomApi;
    private final PlatformTokenStore tokenStore;
    private final SyncJobRepository syncJobRepository;
    private final GameRepository gameRepository;
    private final GameStatAggregator statAggregator;
    private final PerfStatFetcher perfStatFetcher;
    private final ChesscomTokenRefresher tokenRefresher;

    public void process(SyncJob job) {
        Long userId = job.getUserId();
        // Chess.com API는 대소문자 혼용 username에 301을 반환하므로 lowercase로 정규화
        String username = job.getPlatformUsername() != null
            ? job.getPlatformUsername().toLowerCase()
            : null;

        job.start();
        syncJobRepository.save(job);
        log.info("[ChesscomWorker] 수집 시작 userId={} username={} cursor={} jobId={}",
            userId, username, job.getSyncCursor(), job.getId());

        try {
            String accessToken = tokenStore.getChesscomAccessToken(userId);

            ChesscomGameArchivesResponse archivesResponse = chesscomApi.getGameArchives(username);
            List<String> allArchives = archivesResponse.getArchives();

            if (allArchives == null) {
                log.warn("[ChesscomWorker] archives null 응답 — 수집 실패 처리 userId={} username={}", userId, username);
                job.fail("archives_null");
                syncJobRepository.save(job);
                return;
            }

            // cursor가 전월로 설정돼 있으면 당월 아카이브만 처리 (증분 수집)
            List<String> pending = filterPending(allArchives, job.getSyncCursor());
            log.info("[ChesscomWorker] 처리 대상 아카이브 {}개 (cursor={})", pending.size(), job.getSyncCursor());

            List<Game> allNewlySaved = accessToken != null
                ? processParallel(job, userId, username, pending)
                : processSequential(job, userId, username, pending);

            job.complete();
            syncJobRepository.save(job);

            log.info("[ChesscomWorker] 수집 완료 userId={} 신규={}건 total={}건",
                userId, allNewlySaved.size(), job.getTotalFetched());

            if (statAggregator.isAnyStatEmpty(userId, OAuthPlatForm.CHESSCOM)) {
                statAggregator.aggregate(userId, OAuthPlatForm.CHESSCOM);
                perfStatFetcher.fetch(userId, OAuthPlatForm.CHESSCOM, username, null);
            } else if (!allNewlySaved.isEmpty()) {
                statAggregator.aggregateIncremental(userId, OAuthPlatForm.CHESSCOM, allNewlySaved);
                perfStatFetcher.fetch(userId, OAuthPlatForm.CHESSCOM, username, null);
            }

        } catch (Exception e) {
            log.error("[ChesscomWorker] 수집 실패 userId={} error={}", userId, e.getMessage(), e);
            job.fail(e.getMessage());
            syncJobRepository.save(job);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private List<Game> processParallel(SyncJob job, Long userId, String username, List<String> archives) {
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        AtomicInteger failCount = new AtomicInteger(0);
        List<CompletableFuture<List<Game>>> futures;
        try {
            futures = archives.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> {
                    PARALLEL_RATE_LIMITER.acquire();
                    try {
                        return fetchAndSave(job, userId, username, url);
                    } catch (Exception e) {
                        log.warn("[ChesscomWorker] archive 처리 실패 (스킵) url={} error={}", url, e.getMessage());
                        failCount.incrementAndGet();
                        return List.<Game>of();
                    }
                }, executor))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
        }
        if (failCount.get() > 0) {
            log.warn("[ChesscomWorker] 병렬 처리 중 {}개 archive 스킵됨 userId={}", failCount.get(), userId);
        }
        return futures.stream().flatMap(f -> f.join().stream()).toList();
    }

    @SuppressWarnings("UnstableApiUsage")
    private List<Game> processSequential(SyncJob job, Long userId, String username, List<String> archives) {
        List<Game> allSaved = new ArrayList<>();
        int failCount = 0;
        for (String url : archives) {
            FALLBACK_RATE_LIMITER.acquire();
            try {
                allSaved.addAll(fetchAndSave(job, userId, username, url));
            } catch (Exception e) {
                log.warn("[ChesscomWorker] archive 처리 실패 (스킵) url={} error={}", url, e.getMessage());
                failCount++;
            }
        }
        if (failCount > 0) {
            log.warn("[ChesscomWorker] 순차 처리 중 {}개 archive 스킵됨 userId={}", failCount, userId);
        }
        return allSaved;
    }

    private List<Game> fetchAndSave(SyncJob job, Long userId, String username, String url) {
        String token = tokenStore.getChesscomAccessToken(userId);
        String authHeader = token != null ? "Bearer " + token : null;

        ChesscomMonthlyArchiveResponse response;
        try {
            response = fetchArchiveWithRetry(url, authHeader);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.info("[ChesscomWorker] 401 감지, 토큰 갱신 시도 userId={}", userId);
                String newToken = tokenRefresher.refresh(userId);
                if (newToken == null) {
                    job.expireToken();
                    syncJobRepository.save(job);
                    throw new RuntimeException("TOKEN_EXPIRED after refresh failure");
                }
                response = fetchArchiveWithRetry(url, "Bearer " + newToken);
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw e;
            } else {
                log.warn("[ChesscomWorker] HTTP {} 오류, archive 스킵 url={} error={}",
                    e.getStatusCode(), url, e.getMessage());
                throw e;
            }
        } catch (HttpServerErrorException e) {
            log.warn("[ChesscomWorker] 서버 오류 {}, archive 스킵 url={}", e.getStatusCode(), url);
            throw e;
        }

        if (response.getGames() == null || response.getGames().isEmpty()) {
            String yearMonth = extractYearMonth(url);
            job.progressAtomic(yearMonth, 0);
            syncJobRepository.save(job);
            return List.of();
        }

        List<Game> toSave = response.getGames().stream()
            .filter(g -> Boolean.TRUE.equals(g.getRated())
                && g.getTimeClass() != null
                && SUPPORTED_TIME_CLASSES.contains(g.getTimeClass()))
            .map(g -> toGame(userId, username, g))
            .toList();

        List<Game> saved = gameRepository.saveAll(toSave);
        log.info("[ChesscomWorker] {} 아카이브 {}개 수집 → {}개 저장", url, response.getGames().size(), saved.size());

        String yearMonth = extractYearMonth(url);
        job.progressAtomic(yearMonth, saved.size());
        syncJobRepository.save(job);
        return saved;
    }

    private Game toGame(Long userId, String username, ChesscomGameResponse dto) {
        boolean isWhite = username.equalsIgnoreCase(
            dto.getWhite() != null ? dto.getWhite().getUsername() : null
        );
        String playerColor = isWhite ? "WHITE" : "BLACK";
        ChesscomGamePlayerInfo playerInfo = isWhite ? dto.getWhite() : dto.getBlack();
        ChesscomGamePlayerInfo opponentInfo = isWhite ? dto.getBlack() : dto.getWhite();
        String opponentUsername = opponentInfo != null ? opponentInfo.getUsername() : null;

        GameResult result = resolveResult(playerInfo);
        Integer rating = playerInfo != null ? playerInfo.getRating() : null;

        return Game.builder()
            .userId(userId)
            .platform(OAuthPlatForm.CHESSCOM)
            .platformGameId(dto.getUrl())
            .username(username)
            .opponentUsername(opponentUsername)
            .playerColor(playerColor)
            .result(result)
            .timeClass(dto.getTimeClass())
            .timeControl(dto.getTimeControl())
            .rated(dto.getRated())
            .rating(rating)
            .moves(dto.getPgn())
            .variant(dto.getRules())
            .playedAt(epochToLocalDateTime(dto.getEndTime()))
            .createdAt(LocalDateTime.now())
            .build();
    }

    private GameResult resolveResult(ChesscomGamePlayerInfo playerInfo) {
        if (playerInfo == null || playerInfo.getResult() == null) return GameResult.OTHER;
        String r = playerInfo.getResult().toLowerCase();
        if ("win".equals(r)) return GameResult.WIN;
        if (DRAW_RESULTS.contains(r)) return GameResult.DRAW;
        return GameResult.LOSS;
    }

    private LocalDateTime epochToLocalDateTime(Long epochSeconds) {
        if (epochSeconds == null) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    /**
     * cursor(마지막 완료 월 "yyyy/MM") 이후 항목만 반환.
     */
    private List<String> filterPending(List<String> archives, String cursor) {
        if (archives == null) return List.of();
        if (cursor == null) return archives;
        int idx = -1;
        for (int i = 0; i < archives.size(); i++) {
            if (archives.get(i).endsWith(cursor)) {
                idx = i;
                break;
            }
        }
        return idx < 0 ? archives : archives.subList(idx + 1, archives.size());
    }

    /**
     * "https://api.chess.com/pub/player/{username}/games/2024/03" → "2024/03"
     */
    private String extractYearMonth(String url) {
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        return url;
    }

    /**
     * 429 시 BASE_BACKOFF_MS 부터 시작해 BACKOFF_INCREMENT_MS씩 늘리며 MAX_RETRIES 회 재시도.
     * 재시도 소진 시 TooManyRequests 그대로 던짐.
     */
    private ChesscomMonthlyArchiveResponse fetchArchiveWithRetry(String url, String authHeader) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return chesscomApi.getArchiveByUrl(URI.create(url), authHeader);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt < MAX_RETRIES) {
                    long wait = BASE_BACKOFF_MS + (long) attempt * BACKOFF_INCREMENT_MS;
                    log.warn("[ChesscomWorker] 429 수신, {}ms 대기 후 재시도 url={} attempt={}/{}",
                        wait, url, attempt + 1, MAX_RETRIES);
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Backoff sleep interrupted", ie);
                    }
                } else {
                    log.warn("[ChesscomWorker] 429 재시도 소진, archive 스킵 url={}", url);
                    throw e;
                }
            }
        }
        throw new IllegalStateException("unreachable");
    }
}