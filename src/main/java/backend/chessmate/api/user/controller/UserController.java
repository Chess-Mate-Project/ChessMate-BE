package backend.chessmate.api.user.controller;

import backend.chessmate.api.user.dto.TotalUserCountResponse;
import backend.chessmate.api.user.service.UserService;
import backend.chessmate.global.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//
//import backend.chessmate.api.o.config.UserPrincipal;
//import backend.chessmate.api.user.dto.FirstMoveDto;
//import backend.chessmate.api.user.dto.GameSummaryDto;
//import backend.chessmate.api.user.dto.OpeningDto;
//import backend.chessmate.api.user.dto.StreakDto;
//import backend.chessmate.api.user.dto.TierInfoDto;
//import backend.chessmate.api.user.dto.UserPlayCountDto;
//import backend.chessmate.api.user.dto.UserProfileDto;
//import backend.chessmate.domain.user.dto.*;
//import backend.chessmate.api.user.dto.history.UserTierHistoryDto;
//import backend.chessmate.api.user.entity.type.GameType;
//import backend.chessmate.api.user.service.UserService;
//import backend.chessmate.global.common.response.SuccessResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
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
