package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserColorStat;
import com.chessmate.domain.stat.UserColorStatRepository;
import com.chessmate.domain.stat.UserPerfStat;
import com.chessmate.domain.stat.UserPerfStatRepository;
import com.chessmate.external.api.chesscom.ChesscomApi;
import com.chessmate.external.api.lichess.LichessApi;
import com.chessmate.external.dto.account.LichessAccountDto;
import com.chessmate.external.dto.account.PerfDto;
import com.chessmate.external.dto.account.PerfsDto;
import com.chessmate.external.dto.chesscom.ChesscomPlayerStatsResponse;
import com.chessmate.external.dto.chesscom.ChesscomTimeClassStat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 플랫폼 API에서 타임클래스별 레이팅/승패 통계를 가져와 user_perf_stat에 저장.
 *
 * - Lichess: /api/user/{username} → perfs(레이팅+games) + user_color_stat(승/무/패)
 * - Chess.com: /pub/player/{username}/stats → 레이팅 + record(승/무/패)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerfStatFetcher {

    private static final List<String> TIME_CLASSES = List.of("bullet", "blitz", "rapid", "classical");

    private final LichessApi lichessApi;
    private final ChesscomApi chesscomApi;
    private final UserPerfStatRepository perfStatRepository;
    private final UserColorStatRepository colorStatRepository;

    @Transactional
    public void fetch(Long userId, OAuthPlatForm platform, String username) {
        try {
            if (platform == OAuthPlatForm.LICHESS) {
                fetchLichess(userId, username);
            } else {
                fetchChesscom(userId, username);
            }
        } catch (Exception e) {
            log.error("[PerfStatFetcher] 페프 스탯 조회 실패 userId={} platform={} error={}",
                userId, platform, e.getMessage(), e);
        }
    }

    private void fetchLichess(Long userId, String username) {
        LichessAccountDto account = lichessApi.getUser(username);
        PerfsDto perfs = account.perfs();
        if (perfs == null) {
            log.warn("[PerfStatFetcher] Lichess perfs 없음 userId={}", userId);
            return;
        }

        perfStatRepository.deleteByUserIdAndPlatform(userId, OAuthPlatForm.LICHESS);

        List<UserPerfStat> stats = new ArrayList<>();
        for (String timeClass : TIME_CLASSES) {
            PerfDto perf = resolvePerf(perfs, timeClass);
            if (perf == null) continue;

            // 승/무/패는 이미 집계된 user_color_stat에서 추출 (WHITE + BLACK 합산)
            // games도 로컬 데이터 기준으로 사용 → API games와 wins+draws+losses 불일치 방지
            List<UserColorStat> colorStats = colorStatRepository
                .findByUserIdAndPlatformAndTimeClass(userId, OAuthPlatForm.LICHESS, timeClass);

            int wins   = colorStats.stream().mapToInt(UserColorStat::getWins).sum();
            int draws  = colorStats.stream().mapToInt(UserColorStat::getDraws).sum();
            int losses = colorStats.stream().mapToInt(UserColorStat::getLosses).sum();
            int games  = wins + draws + losses;

            // 로컬에 집계된 게임도 없고 API에도 0이면 저장 불필요
            if (games == 0 && perf.games() == 0) continue;

            stats.add(UserPerfStat.builder()
                .userId(userId)
                .platform(OAuthPlatForm.LICHESS)
                .timeClass(timeClass)
                .rating(perf.rating())
                .games(games)
                .wins(wins)
                .losses(losses)
                .draws(draws)
                .build());
        }

        perfStatRepository.saveAll(stats);
        log.info("[PerfStatFetcher] Lichess perf 저장 userId={} {}타입", userId, stats.size());
    }

    private void fetchChesscom(Long userId, String username) {
        ChesscomPlayerStatsResponse statsResponse = chesscomApi.getPlayerStats(username);

        perfStatRepository.deleteByUserIdAndPlatform(userId, OAuthPlatForm.CHESSCOM);

        Map<String, ChesscomTimeClassStat> timeClassMap = new HashMap<>();
        timeClassMap.put("bullet", statsResponse.chessBullet());
        timeClassMap.put("blitz",  statsResponse.chessBlitz());
        timeClassMap.put("rapid",  statsResponse.chessRapid());

        List<UserPerfStat> stats = new ArrayList<>();
        for (Map.Entry<String, ChesscomTimeClassStat> entry : timeClassMap.entrySet()) {
            ChesscomTimeClassStat tcs = entry.getValue();
            if (tcs == null || tcs.last() == null) continue;

            int wins   = tcs.record() != null ? tcs.record().win()  : 0;
            int losses = tcs.record() != null ? tcs.record().loss() : 0;
            int draws  = tcs.record() != null ? tcs.record().draw() : 0;

            stats.add(UserPerfStat.builder()
                .userId(userId)
                .platform(OAuthPlatForm.CHESSCOM)
                .timeClass(entry.getKey())
                .rating(tcs.last().rating())
                .games(wins + losses + draws)
                .wins(wins)
                .losses(losses)
                .draws(draws)
                .build());
        }

        perfStatRepository.saveAll(stats);
        log.info("[PerfStatFetcher] Chess.com perf 저장 userId={} {}타입", userId, stats.size());
    }

    private PerfDto resolvePerf(PerfsDto perfs, String timeClass) {
        return switch (timeClass) {
            case "bullet"    -> perfs.bullet();
            case "blitz"     -> perfs.blitz();
            case "rapid"     -> perfs.rapid();
            case "classical" -> perfs.classical();
            default          -> null;
        };
    }

    private ChesscomTimeClassStat nullStat() {
        return null;
    }
}
