package backend.chessmate.domain.user.controller;

import backend.chessmate.domain.auth.config.UserPrincipal;
import backend.chessmate.domain.user.dto.*;
import backend.chessmate.domain.user.entity.FirstMove;
import backend.chessmate.domain.user.entity.Opening;
import backend.chessmate.domain.user.entity.Streak;
import backend.chessmate.global.common.response.SuccessResponse;

import backend.chessmate.domain.user.dto.response.streak.UserStreaksResponse;

import backend.chessmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/streak")
    public ResponseEntity<SuccessResponse<List<StreakDto>>> getUserStreaks(
            @AuthenticationPrincipal UserPrincipal u,
            @RequestParam(required  = false) int year
    ) {

        List<StreakDto> response = userService.getStreak(u.getUser(), year);

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 스트릭 조회 성공" , response)
        );
    }

    @GetMapping("/move")
    public ResponseEntity<SuccessResponse<List<FirstMoveDto>>> getUserEco(
            @AuthenticationPrincipal UserPrincipal u,
            @RequestParam(required  = false, defaultValue = "5") int top) { // 기본은 탑 5까지만

        List<FirstMoveDto> moves = userService.getFirstMove(u.getUser(), top);

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 첫 수 통계 조회 성공" , moves)
        );
    }

    @GetMapping("/opening")
    public ResponseEntity<SuccessResponse<List<OpeningDto>>> getUserOpening(
            @AuthenticationPrincipal UserPrincipal u,
            @RequestParam(required  = false, defaultValue = "5") int top) {

        List<OpeningDto> openings = userService.getOpening(u.getUser(), top);

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 오프닝 통계 조회 성공" , openings)
        );
    }

    @GetMapping("/count")
    public ResponseEntity<SuccessResponse<UserPlayCountDto>> getUserPlays(
            @AuthenticationPrincipal UserPrincipal u
    ) {

        UserPlayCountDto response = userService.getPlayCount(u.getUser());

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 플레이 카운트 조회 성공", response)
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<SuccessResponse<?>> getSummaryByGameType(
            @AuthenticationPrincipal UserPrincipal u
    ) {

        GameSummaryDto response = userService.getGameSummary(u.getUser());

        return ResponseEntity.ok(
                new SuccessResponse<>("사용자 게임 타입별 통계 조회 성공", response)
        );
    }












}
