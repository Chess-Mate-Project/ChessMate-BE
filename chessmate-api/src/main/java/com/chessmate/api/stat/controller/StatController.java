package com.chessmate.api.stat.controller;

import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.stat.dto.ColorStatResponse;
import com.chessmate.api.stat.dto.FirstMoveStatResponse;
import com.chessmate.api.stat.dto.RatingHistoryResponse;
import com.chessmate.api.stat.dto.StreakResponse;
import com.chessmate.api.stat.dto.UserPerfStatResponse;
import com.chessmate.api.stat.service.StatService;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.response.SuccessResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    /**
     * 게임 스트릭 (일별 게임 수, 연도별 그룹)
     * GET /api/stat/streak?platform=LICHESS          → 전체 연도
     * GET /api/stat/streak?platform=LICHESS&year=2024 → 2024년만
     */
    @GetMapping("/streak")
    public ResponseEntity<SuccessResponse<StreakResponse>> getStreak(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(new SuccessResponse<>("게임 스트릭 조회 성공",
            statService.getStreak(principal.getId(), platform, year)));
    }

    /**
     * 색상별 게임 통계
     * GET /api/stat/color?platform=LICHESS[&timeClass=blitz]
     */
    @GetMapping("/color")
    public ResponseEntity<SuccessResponse<List<ColorStatResponse>>> getColorStat(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        return ResponseEntity.ok(new SuccessResponse<>("색상별 게임 통계 조회 성공",
            statService.getColorStat(principal.getId(), platform, timeClass)));
    }

    /**
     * 첫 수 통계
     * GET /api/stat/first-move?platform=LICHESS[&timeClass=blitz]
     */
    @GetMapping("/first-move")
    public ResponseEntity<SuccessResponse<List<FirstMoveStatResponse>>> getFirstMoveStat(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        return ResponseEntity.ok(new SuccessResponse<>("첫 수 통계 조회 성공",
            statService.getFirstMoveStat(principal.getId(), platform, timeClass)));
    }

    /**
     * 타임클래스별 레이팅 + 승/무/패 통계
     * GET /api/stat/perf?platform=LICHESS
     * GET /api/stat/perf?platform=LICHESS&timeClass=blitz
     */
    @GetMapping("/perf")
    public ResponseEntity<SuccessResponse<List<UserPerfStatResponse>>> getPerfStats(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        return ResponseEntity.ok(new SuccessResponse<>("퍼프 통계 조회 성공",
            statService.getPerfStats(principal.getId(), platform, timeClass)));
    }

    /**
     * 최근 1년간 월별 레이팅 변화
     * GET /api/stat/rating-history?platform=LICHESS              → 전체 타임클래스
     * GET /api/stat/rating-history?platform=LICHESS&timeClass=blitz → blitz만
     */
    @GetMapping("/rating-history")
    public ResponseEntity<SuccessResponse<RatingHistoryResponse>> getRatingHistory(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        return ResponseEntity.ok(new SuccessResponse<>("레이팅 히스토리 조회 성공",
            statService.getRatingHistory(principal.getId(), platform, timeClass)));
    }
}
