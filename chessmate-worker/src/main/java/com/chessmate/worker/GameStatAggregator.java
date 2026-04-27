package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.Game;
import com.chessmate.domain.game.GameRepository;
import com.chessmate.domain.game.GameResult;
import com.chessmate.domain.stat.UserColorStat;
import com.chessmate.domain.stat.UserColorStatRepository;
import com.chessmate.domain.stat.UserDailyGameStat;
import com.chessmate.domain.stat.UserDailyGameStatRepository;
import com.chessmate.domain.stat.UserFirstMoveStat;
import com.chessmate.domain.stat.UserFirstMoveStatRepository;
import com.chessmate.domain.stat.UserPerfStatRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게임 수집 완료 후 집계 통계를 계산하는 컴포넌트.
 *
 * 두 가지 집계 전략:
 * - aggregate()            : full recompute (초기 전체 수집 / 긴급 재집계용)
 * - aggregateIncremental() : 증분 업데이트 (30분 스케줄 증분 sync용)
 *
 * 집계 항목:
 * - user_daily_game_stat   : 날짜별 총/승/무/패
 * - user_color_stat        : (timeClass x 색상)별 승/무/패
 * - user_first_move_stat   : (timeClass x 색상 x 첫수)별 count
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameStatAggregator {

    private final GameRepository gameRepository;
    private final UserDailyGameStatRepository dailyStatRepository;
    private final UserColorStatRepository colorStatRepository;
    private final UserFirstMoveStatRepository firstMoveStatRepository;
    private final UserPerfStatRepository perfStatRepository;

    /**
     * stat 테이블 중 하나라도 비어 있으면 true.
     * 수동 삭제 / 집계 실패 등 긴급 상황 감지용.
     */
    public boolean isAnyStatEmpty(Long userId, OAuthPlatForm platform) {
        return !firstMoveStatRepository.existsByUserIdAndPlatform(userId, platform)
            || !colorStatRepository.existsByUserIdAndPlatform(userId, platform)
            || !dailyStatRepository.existsByUserIdAndPlatform(userId, platform)
            || !perfStatRepository.existsByUserIdAndPlatform(userId, platform);
    }

    /**
     * 증분 집계: 새로 저장된 게임만 기존 stat에 더한다.
     * Game 테이블 전체 로드 없이 신규 게임 수에 비례하는 DB 접근만 발생한다.
     */
    @Transactional
    public void aggregateIncremental(Long userId, OAuthPlatForm platform, List<Game> newGames) {
        List<Game> rated = newGames.stream()
            .filter(g -> Boolean.TRUE.equals(g.getRated()))
            .toList();
        if (rated.isEmpty()) return;

        log.info("[Aggregator] incremental aggregate userId={} platform={} newGames={}", userId, platform, rated.size());
        incrementDailyStats(userId, platform, rated);
        incrementColorStats(userId, platform, rated);
        incrementFirstMoveStats(userId, platform, rated);
    }

    /**
     * 전체 재집계: Game 테이블 전체를 읽어 stat을 delete → 재계산 → saveAll.
     * 초기 전체 수집(processFullSync) 및 stat 테이블 긴급 복구 시 사용.
     */
    @Transactional
    public void aggregate(Long userId, OAuthPlatForm platform) {
        log.info("[Aggregator] full aggregate start userId={} platform={}", userId, platform);

        List<Game> allGames = gameRepository.findByUserIdAndPlatform(userId, platform);
        if (allGames.isEmpty()) {
            log.info("[Aggregator] no games to aggregate userId={}", userId);
            return;
        }

        List<Game> ratedGames = allGames.stream()
            .filter(g -> Boolean.TRUE.equals(g.getRated()))
            .toList();

        log.info("[Aggregator] total={} rated={} userId={} platform={}",
            allGames.size(), ratedGames.size(), userId, platform);

        computeDailyStats(userId, platform, ratedGames);
        computeColorStats(userId, platform, ratedGames);
        computeFirstMoveStats(userId, platform, ratedGames);

        log.info("[Aggregator] full aggregate done userId={} platform={} rated={}", userId, platform, ratedGames.size());
    }

    // ======================== Full Recompute ========================

    private void computeDailyStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<LocalDate, int[]> map = new HashMap<>();
        for (Game game : games) {
            if (game.getPlayedAt() == null) continue;
            LocalDate date = game.getPlayedAt().toLocalDate();
            int[] counts = map.computeIfAbsent(date, d -> new int[4]);
            counts[0]++;
            if (GameResult.WIN == game.getResult())        counts[1]++;
            else if (GameResult.DRAW == game.getResult())  counts[2]++;
            else if (GameResult.LOSS == game.getResult())  counts[3]++;
        }

        List<UserDailyGameStat> stats = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> e : map.entrySet()) {
            int[] c = e.getValue();
            stats.add(UserDailyGameStat.builder()
                .userId(userId).platform(platform).date(e.getKey())
                .total(c[0]).wins(c[1]).draws(c[2]).losses(c[3])
                .build());
        }

        dailyStatRepository.deleteByUserIdAndPlatform(userId, platform);
        dailyStatRepository.saveAll(stats);
        log.debug("[Aggregator] daily stat saved count={}", stats.size());
    }

    private void computeColorStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<String, int[]> map = new HashMap<>();
        for (Game game : games) {
            if (game.getTimeClass() == null || game.getPlayerColor() == null) continue;
            String key = game.getTimeClass() + ":" + game.getPlayerColor();
            int[] counts = map.computeIfAbsent(key, k -> new int[3]);
            if (GameResult.WIN == game.getResult())        counts[0]++;
            else if (GameResult.DRAW == game.getResult())  counts[1]++;
            else if (GameResult.LOSS == game.getResult())  counts[2]++;
        }

        List<UserColorStat> stats = new ArrayList<>();
        for (Map.Entry<String, int[]> e : map.entrySet()) {
            String[] parts = e.getKey().split(":");
            int[] c = e.getValue();
            stats.add(UserColorStat.builder()
                .userId(userId).platform(platform)
                .timeClass(parts[0]).color(parts[1])
                .wins(c[0]).draws(c[1]).losses(c[2])
                .build());
        }

        colorStatRepository.deleteByUserIdAndPlatform(userId, platform);
        colorStatRepository.saveAll(stats);
        log.debug("[Aggregator] color stat saved count={}", stats.size());
    }

    private void computeFirstMoveStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<String, Integer> map = new HashMap<>();
        for (Game game : games) {
            if (game.getTimeClass() == null || game.getPlayerColor() == null
                || game.getMoves() == null || game.getMoves().isBlank()) continue;
            String firstMove = extractFirstMove(game.getMoves(), game.getPlayerColor(), platform);
            if (firstMove == null) continue;
            String key = game.getTimeClass() + ":" + game.getPlayerColor() + ":" + firstMove;
            map.merge(key, 1, Integer::sum);
        }

        List<UserFirstMoveStat> stats = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            String[] parts = e.getKey().split(":");
            stats.add(UserFirstMoveStat.builder()
                .userId(userId).platform(platform)
                .timeClass(parts[0]).color(parts[1]).move(parts[2])
                .count(e.getValue())
                .build());
        }

        firstMoveStatRepository.deleteByUserIdAndPlatform(userId, platform);
        firstMoveStatRepository.saveAll(stats);
        log.debug("[Aggregator] first move stat saved count={}", stats.size());
    }

    // ======================== Incremental Update ========================

    private void incrementDailyStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<LocalDate, int[]> delta = new HashMap<>();
        for (Game g : games) {
            if (g.getPlayedAt() == null) continue;
            LocalDate date = g.getPlayedAt().toLocalDate();
            int[] c = delta.computeIfAbsent(date, d -> new int[4]);
            c[0]++;
            if (GameResult.WIN == g.getResult())        c[1]++;
            else if (GameResult.DRAW == g.getResult())  c[2]++;
            else if (GameResult.LOSS == g.getResult())  c[3]++;
        }

        List<UserDailyGameStat> toSave = new ArrayList<>();
        for (var entry : delta.entrySet()) {
            LocalDate date = entry.getKey();
            int[] d = entry.getValue();
            var existing = dailyStatRepository.findByUserIdAndPlatformAndDate(userId, platform, date);
            if (existing.isPresent()) {
                var e = existing.get();
                toSave.add(UserDailyGameStat.builder()
                    .id(e.getId()).userId(userId).platform(platform).date(date)
                    .total(e.getTotal() + d[0]).wins(e.getWins() + d[1])
                    .draws(e.getDraws() + d[2]).losses(e.getLosses() + d[3])
                    .build());
            } else {
                toSave.add(UserDailyGameStat.builder()
                    .userId(userId).platform(platform).date(date)
                    .total(d[0]).wins(d[1]).draws(d[2]).losses(d[3])
                    .build());
            }
        }
        dailyStatRepository.saveAll(toSave);
    }

    private void incrementColorStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<String, int[]> delta = new HashMap<>();
        for (Game g : games) {
            if (g.getTimeClass() == null || g.getPlayerColor() == null) continue;
            String key = g.getTimeClass() + ":" + g.getPlayerColor();
            int[] c = delta.computeIfAbsent(key, k -> new int[3]);
            if (GameResult.WIN == g.getResult())        c[0]++;
            else if (GameResult.DRAW == g.getResult())  c[1]++;
            else if (GameResult.LOSS == g.getResult())  c[2]++;
        }

        List<UserColorStat> toSave = new ArrayList<>();
        for (var entry : delta.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String tc = parts[0], color = parts[1];
            int[] d = entry.getValue();
            var existing = colorStatRepository.findByUserIdAndPlatformAndTimeClassAndColor(userId, platform, tc, color);
            if (existing.isPresent()) {
                var e = existing.get();
                toSave.add(UserColorStat.builder()
                    .id(e.getId()).userId(userId).platform(platform).timeClass(tc).color(color)
                    .wins(e.getWins() + d[0]).draws(e.getDraws() + d[1]).losses(e.getLosses() + d[2])
                    .build());
            } else {
                toSave.add(UserColorStat.builder()
                    .userId(userId).platform(platform).timeClass(tc).color(color)
                    .wins(d[0]).draws(d[1]).losses(d[2])
                    .build());
            }
        }
        colorStatRepository.saveAll(toSave);
    }

    private void incrementFirstMoveStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<String, Integer> delta = new HashMap<>();
        for (Game g : games) {
            if (g.getTimeClass() == null || g.getPlayerColor() == null
                || g.getMoves() == null || g.getMoves().isBlank()) continue;
            String firstMove = extractFirstMove(g.getMoves(), g.getPlayerColor(), platform);
            if (firstMove == null) continue;
            String key = g.getTimeClass() + ":" + g.getPlayerColor() + ":" + firstMove;
            delta.merge(key, 1, Integer::sum);
        }

        List<UserFirstMoveStat> toSave = new ArrayList<>();
        for (var entry : delta.entrySet()) {
            String[] parts = entry.getKey().split(":");
            String tc = parts[0], color = parts[1], move = parts[2];
            int d = entry.getValue();
            var existing = firstMoveStatRepository.findByUserIdAndPlatformAndTimeClassAndColorAndMove(userId, platform, tc, color, move);
            if (existing.isPresent()) {
                var e = existing.get();
                toSave.add(UserFirstMoveStat.builder()
                    .id(e.getId()).userId(userId).platform(platform)
                    .timeClass(tc).color(color).move(move)
                    .count(e.getCount() + d)
                    .build());
            } else {
                toSave.add(UserFirstMoveStat.builder()
                    .userId(userId).platform(platform)
                    .timeClass(tc).color(color).move(move)
                    .count(d)
                    .build());
            }
        }
        firstMoveStatRepository.saveAll(toSave);
    }

    // ======================== First Move Parser ========================

    private String extractFirstMove(String moves, String playerColor, OAuthPlatForm platform) {
        try {
            if (platform == OAuthPlatForm.LICHESS) {
                return extractLichessFirstMove(moves, playerColor);
            } else {
                return extractChesscomFirstMove(moves, playerColor);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String extractLichessFirstMove(String moves, String playerColor) {
        String[] tokens = moves.trim().split("\\s+");
        if ("WHITE".equals(playerColor)) {
            return tokens.length > 0 ? tokens[0] : null;
        } else {
            return tokens.length > 1 ? tokens[1] : null;
        }
    }

    private String extractChesscomFirstMove(String pgn, String playerColor) {
        int movesStart = pgn.lastIndexOf("\n\n");
        String movesSection = movesStart >= 0
            ? pgn.substring(movesStart).trim()
            : pgn.trim();

        String cleaned = movesSection.replaceAll("\\{[^}]*\\}", "");
        cleaned = cleaned.replaceAll("\\d+\\.{1,3}", "").trim();
        String[] tokens = cleaned.trim().split("\\s+");

        if ("WHITE".equals(playerColor)) {
            return isValidMove(tokens, 0) ? tokens[0] : null;
        } else {
            return isValidMove(tokens, 1) ? tokens[1] : null;
        }
    }

    private boolean isValidMove(String[] tokens, int idx) {
        if (tokens.length <= idx) return false;
        String t = tokens[idx];
        return !t.matches(".*[-*].*") && !t.equals("1/2");
    }
}