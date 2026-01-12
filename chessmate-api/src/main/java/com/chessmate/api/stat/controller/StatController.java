package com.chessmate.api.stat.controller;

import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.stat.dto.YearStreakDto;
import com.chessmate.api.stat.service.StatService;
import com.chessmate.common.response.SuccessResponse;
import java.time.Year;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
  public ResponseEntity<SuccessResponse<Void>> getColorStats(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {

    statService.getColorStats(userPrincipal.getUser());
    return ResponseEntity.ok(
        new SuccessResponse<>("Color Stats 조회 성공", null)
    );
  }
}


