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
 * 게임 수집 완료 후 집계 통계를 재계산하는 컴포넌트.
 *
 * 집계 항목:
 * - user_daily_game_stat: 날짜별 총/승/무/패
 * - user_color_stat: (timeClass × 색상)별 승/무/패
 * - user_first_move_stat: (timeClass × 색상 × 첫수)별 count
 * - lichess_user_stat: Lichess 사용자 요약 통계 (rated 게임 기준)
 *
 * 전략: full recompute (delete → saveAll)
 *
 * Lichess 주의사항:
 * - API에서 rated=true 파라미터로 레이팅 게임만 수집
 * - 따라서 game 테이블의 Lichess 게임은 전부 rated
 * - lichess_user_stat의 all_games = rated_games = game 테이블 집계 결과
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
     * 해당 유저/플랫폼에 대해 stat 테이블 중 하나라도 비어 있으면 true.
     * 신규 게임 없이도 강제 재집계가 필요한 상황(수동 삭제, 집계 실패 등) 감지용.
     */
    public boolean isAnyStatEmpty(Long userId, OAuthPlatForm platform) {
        return !firstMoveStatRepository.existsByUserIdAndPlatform(userId, platform)
            || !colorStatRepository.existsByUserIdAndPlatform(userId, platform)
            || !dailyStatRepository.existsByUserIdAndPlatform(userId, platform)
            || !perfStatRepository.existsByUserIdAndPlatform(userId, platform);
    }

    @Transactional
    public void aggregate(Long userId, OAuthPlatForm platform) {
        log.info("[Aggregator] 집계 시작 userId={} platform={}", userId, platform);

        List<Game> allGames = gameRepository.findByUserIdAndPlatform(userId, platform);
        if (allGames.isEmpty()) {
            log.info("[Aggregator] 집계할 게임 없음 userId={}", userId);
            return;
        }

        // 스탯 집계는 레이팅 게임만 사용
        List<Game> ratedGames = allGames.stream()
            .filter(g -> Boolean.TRUE.equals(g.getRated()))
            .toList();

        log.info("[Aggregator] 전체={}건 / rated={}건 userId={} platform={}",
            allGames.size(), ratedGames.size(), userId, platform);

        computeDailyStats(userId, platform, ratedGames);
        computeColorStats(userId, platform, ratedGames);
        computeFirstMoveStats(userId, platform, ratedGames);

        log.info("[Aggregator] 집계 완료 userId={} platform={} rated={}", userId, platform, ratedGames.size());
    }

    // ======================== Daily Stats ========================

    private void computeDailyStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<LocalDate, int[]> map = new HashMap<>(); // [total, wins, draws, losses]

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
        log.debug("[Aggregator] daily stat {}건 저장", stats.size());
    }

    // ======================== Color Stats ========================

    private void computeColorStats(Long userId, OAuthPlatForm platform, List<Game> games) {
        Map<String, int[]> map = new HashMap<>(); // [wins, draws, losses]

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
        log.debug("[Aggregator] color stat {}건 저장", stats.size());
    }

    // ======================== First Move Stats ========================

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
        log.debug("[Aggregator] first move stat {}건 저장", stats.size());
    }

    // ======================== First Move 파싱 ========================

    /**
     * 첫 수 추출.
     *
     * Lichess: moves = "e4 e5 Nf3 Nc6 ..."
     *   WHITE → 0번째 토큰, BLACK → 1번째 토큰
     *
     * Chess.com: moves = PGN (헤더 + 수순)
     *   "1. e4 e5 2. Nf3 ..." 형식에서 파싱
     */
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

        // {[%clk ...]} 같은 중괄호 주석 제거
        String cleaned = movesSection.replaceAll("\\{[^}]*\\}", "");
        // "1." 또는 "1..." 형식의 수 번호 제거
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
