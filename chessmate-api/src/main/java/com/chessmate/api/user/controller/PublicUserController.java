package com.chessmate.api.user.controller;

import com.chessmate.api.stat.dto.ColorStatResponse;
import com.chessmate.api.stat.dto.FirstMoveStatResponse;
import com.chessmate.api.stat.dto.RatingHistoryResponse;
import com.chessmate.api.stat.dto.StreakResponse;
import com.chessmate.api.stat.dto.UserPerfStatResponse;
import com.chessmate.api.stat.service.StatService;
import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.api.user.dto.SearchUsersResponse;
import com.chessmate.api.user.service.UserService;
import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.response.SuccessResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{username}")
@RequiredArgsConstructor
public class PublicUserController {

    private final UserService userService;
    private final StatService statService;

  /**
   * 타인 검색
   * GET /api/users?platform=LICHESS&username=username
   * 여기서 사용하는 username은 keyword임
   * */
  @GetMapping
  public ResponseEntity<SuccessResponse<SearchUsersResponse>> getUser(
      @PathVariable String username,
      @RequestParam OAuthPlatForm platform,
      @AuthenticationPrincipal UserPrincipal principal
  ) {
    Long excludeUserId = platform == principal.getProvider() ? principal.getId() : null;
    SearchUsersResponse response = userService.searchUsers(username, platform, excludeUserId);

    return ResponseEntity.ok(new SuccessResponse<>(
        "유저 검색 성공",
        response
    ));
  }

    /**
     * 타인 프로필 조회
     * GET /api/users/{username}/profile?platform=LICHESS
     */
    @GetMapping("/profile")
    public ResponseEntity<SuccessResponse<ProfileResponse>> getProfile(
        @PathVariable String username,
        @RequestParam OAuthPlatForm platform
    ) {
        Long userId = userService.resolveUserId(username, platform);
        return ResponseEntity.ok(new SuccessResponse<>(
            "프로필 조회 성공",
            userService.getProfile(userId, platform)
        ));
    }

    /**
     * 타인 레이팅 + 승/무/패 통계
     * GET /api/users/{username}/stats/perf?platform=LICHESS[&timeClass=blitz]
     */
    @GetMapping("/stats/perf")
    public ResponseEntity<SuccessResponse<List<UserPerfStatResponse>>> getPerfStats(
        @PathVariable String username,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        Long userId = userService.resolveUserId(username, platform);
        return ResponseEntity.ok(new SuccessResponse<>(
            "퍼프 통계 조회 성공",
            statService.getPerfStats(userId, platform, timeClass)
        ));
    }

    /**
     * 타인 게임 스트릭
     * GET /api/users/{username}/stats/streak?platform=LICHESS[&year=2024]
     */
    @GetMapping("/stats/streak")
    public ResponseEntity<SuccessResponse<StreakResponse>> getStreak(
        @PathVariable String username,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) Integer year
    ) {
        Long userId = userService.resolveUserId(username, platform);
        return ResponseEntity.ok(new SuccessResponse<>(
            "게임 스트릭 조회 성공",
            statService.getStreak(userId, platform, year)
        ));
    }

    /**
     * 타인 색상별 통계
     * GET /api/users/{username}/stats/color?platform=LICHESS[&timeClass=blitz]
     */
    @GetMapping("/stats/color")
    public ResponseEntity<SuccessResponse<List<ColorStatResponse>>> getColorStat(
        @PathVariable String username,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        Long userId = userService.resolveUserId(username, platform);
        return ResponseEntity.ok(new SuccessResponse<>(
            "색상별 통계 조회 성공",
            statService.getColorStat(userId, platform, timeClass)
        ));
    }

    /**
     * 타인 첫 수 통계
     * GET /api/users/{username}/stats/first-move?platform=LICHESS[&timeClass=blitz]
     */
    @GetMapping("/stats/first-move")
    public ResponseEntity<SuccessResponse<List<FirstMoveStatResponse>>> getFirstMoveStat(
        @PathVariable String username,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        Long userId = userService.resolveUserId(username, platform);
        return ResponseEntity.ok(new SuccessResponse<>(
            "첫 수 통계 조회 성공",
            statService.getFirstMoveStat(userId, platform, timeClass)
        ));
    }

    /**
     * 타인 월별 레이팅 변화
     * GET /api/users/{username}/stats/rating-history?platform=LICHESS[&timeClass=blitz]
     */
    @GetMapping("/stats/rating-history")
    public ResponseEntity<SuccessResponse<RatingHistoryResponse>> getRatingHistory(
        @PathVariable String username,
        @RequestParam OAuthPlatForm platform,
        @RequestParam(required = false) String timeClass
    ) {
        Long userId = userService.resolveUserId(username, platform);
        return ResponseEntity.ok(new SuccessResponse<>(
            "레이팅 히스토리 조회 성공",
            statService.getRatingHistory(userId, platform, timeClass)
        ));
    }
}