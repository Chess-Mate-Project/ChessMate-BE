package com.chessmate.api.user.controller;


import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.user.dto.TotalUserCountResponse;
import com.chessmate.api.user.dto.UpdateUserDescriptionRequest;
import com.chessmate.api.user.service.UserService;
import com.chessmate.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("count")
  public ResponseEntity<SuccessResponse<TotalUserCountResponse>> getTotalUserCount() {
    TotalUserCountResponse response = userService.getTotalUserCount();

    return ResponseEntity.ok(
        new SuccessResponse<>("총 사용자 수 조회 성공", response)
    );
  }

  @PutMapping("/description")
  public ResponseEntity<SuccessResponse<Void>> updateUserDescription(
      @AuthenticationPrincipal UserPrincipal u,
      @RequestBody UpdateUserDescriptionRequest request
  ) {
    userService.updateUserDescription(u.getUser(), request);
    return ResponseEntity.ok(
        new SuccessResponse<>("사용자 소개글 수정 성공", null)
    );
  }



//
//    private final UserService userService;
//
//
//    @GetMapping("/streak")
//    public ResponseEntity<SuccessResponse<List<StreakDto>>> getUserStreaks(
//            @AuthenticationPrincipal UserPrincipal u,
//            @RequestParam(required  = false) int year
//    ) {
//
//        List<StreakDto> response = userService.getStreak(u.getUser(), year);
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 스트릭 조회 성공" , response)
//        );
//    }
//
//    @GetMapping("/move")
//    public ResponseEntity<SuccessResponse<List<FirstMoveDto>>> getUserEco(
//            @AuthenticationPrincipal UserPrincipal u,
//            @RequestParam(required  = false, defaultValue = "5") int top) { // 기본은 탑 5까지만
//
//        List<FirstMoveDto> moves = userService.getFirstMove(u.getUser(), top);
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 첫 수 통계 조회 성공" , moves)
//        );
//    }
//
//    @GetMapping("/opening")
//    public ResponseEntity<SuccessResponse<List<OpeningDto>>> getUserOpening(
//            @AuthenticationPrincipal UserPrincipal u,
//            @RequestParam(required  = false, defaultValue = "5") int top) {
//
//        List<OpeningDto> openings = userService.getOpening(u.getUser(), top);
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 오프닝 통계 조회 성공" , openings)
//        );
//    }
//
//    @GetMapping("/count")
//    public ResponseEntity<SuccessResponse<UserPlayCountDto>> getUserPlays(
//            @AuthenticationPrincipal UserPrincipal u
//    ) {
//
//        UserPlayCountDto response = userService.getPlayCount(u.getUser());
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 플레이 카운트 조회 성공", response)
//        );
//    }
//
//    @GetMapping("/summary")
//    public ResponseEntity<SuccessResponse<GameSummaryDto>> getSummaryByGameType(
//            @AuthenticationPrincipal UserPrincipal u
//    ) {
//
//        GameSummaryDto response = userService.getGameSummary(u.getUser());
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 게임 타입별 통계 조회 성공", response)
//        );
//    }
//
//    @GetMapping("/tier")
//    public ResponseEntity<SuccessResponse<TierInfoDto>> getTierInfo(
//            @AuthenticationPrincipal UserPrincipal u
//    ) {
//
//        TierInfoDto response = userService.getTierInfo(u.getUser());
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 티어 정보 조회 성공", response)
//        );
//    }
//
//    @GetMapping("/profile")
//    public ResponseEntity<SuccessResponse<UserProfileDto>> getUserProfile(
//            @AuthenticationPrincipal UserPrincipal u
//    ) {
//
//        UserProfileDto response = userService.getUserProfile(u.getUser());
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 정보 조회 성공", response)
//        );
//    }
//
//    @GetMapping("/tier-history")
//    public ResponseEntity<SuccessResponse<UserTierHistoryDto>> getUserRatingHistory(
//            @AuthenticationPrincipal UserPrincipal u,
//            @RequestParam(name = "gameType") GameType gameType
//    ) {
//
//        UserTierHistoryDto response = userService.getUserRatingHistory(u.getUser(), gameType);
//
//        return ResponseEntity.ok(
//                new SuccessResponse<>("사용자 게임 타입별 티어 변동 이력 조회 성공", response)
//        );
//    }
//
//
//
}
