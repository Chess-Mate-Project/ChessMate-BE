package com.chessmate.api.stat.controller;

import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.stat.dto.ColorStatsResponse;
import com.chessmate.api.stat.dto.FirstMoveResponse;
import com.chessmate.api.stat.dto.TierResponse;
import com.chessmate.api.stat.dto.UserPerfResponse;
import com.chessmate.api.stat.dto.YearStreakDto;
import com.chessmate.api.stat.service.StatService;
import com.chessmate.common.response.SuccessResponse;
import com.chessmate.common.type.GameType;
import com.chessmate.infra_redis.redis.CacheService;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stat")
public class StatController {

  private final StatService statService;

  @GetMapping("/streak")
  public ResponseEntity<SuccessResponse<YearStreakDto>> getStreak(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam Year year
  ) {

    var statDto = statService.getDailyStreaksByYear(userPrincipal.getUser(), year);

    return ResponseEntity.ok(
        new SuccessResponse<>("Streak 조회 성공", statDto)
    );
  }

  @GetMapping("/color")
  public ResponseEntity<SuccessResponse<ColorStatsResponse>> getColorStats(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(defaultValue = "RAPID") GameType gameType
  ) {

    ColorStatsResponse response = statService.getColorStats(userPrincipal.getUser(), gameType);

    return ResponseEntity.ok(
        new SuccessResponse<>("Color Stats 조회 성공", response)
    );
  }

  @GetMapping("/perf")
  public ResponseEntity<SuccessResponse<UserPerfResponse>> getUserPerf(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(defaultValue = "RAPID") GameType gameType
  ) {

    UserPerfResponse response = statService.getUserPerf(userPrincipal.getUser(), gameType);

    if (response == null) {
      return ResponseEntity.ok(
          new SuccessResponse<>("UserPerf 데이터 없음", null)
      );
    }

    return ResponseEntity.ok(
        new SuccessResponse<>("UserPerf 정보 조회 성공", response)
    );
  }

  @GetMapping("/first-move")
  public ResponseEntity<SuccessResponse<FirstMoveResponse>> getFirstMoveStats(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(defaultValue = "RAPID") GameType gameType
  ) {

    FirstMoveResponse response = statService.getFirstMoveStats(userPrincipal.getUser(), gameType);

    return ResponseEntity.ok(
        new SuccessResponse<>("Tier Stats 조회 성공", response)
    );
  }

  @PutMapping("force-refresh")
  public ResponseEntity<SuccessResponse<TierResponse>> forceRefresh(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    statService.forceUpdateUserData(userPrincipal.getUser());

    return ResponseEntity.ok(
        new SuccessResponse<>("사용자 데이터 강제 갱신 요청 성공", null)
    );
  }
}


