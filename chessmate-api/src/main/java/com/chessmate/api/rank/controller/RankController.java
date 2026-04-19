package com.chessmate.api.rank.controller;

import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.rank.dto.PlatformUserCountResponse;
import com.chessmate.api.rank.dto.RankingResponse;
import com.chessmate.api.rank.service.RankService;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.response.SuccessResponse;
import com.chessmate.common.type.GameType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rank")
@Slf4j
public class RankController {

  private final RankService rankService;

  @GetMapping("/ranking")
  public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(defaultValue = "RAPID") GameType gameType,
      @RequestParam OAuthPlatForm platform,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    Long userId = userPrincipal != null ? userPrincipal.getId() : null;
    OAuthPlatForm userProvider = userPrincipal != null ? userPrincipal.getProvider() : null;

    RankingResponse response = rankService.getRankers(userId, userProvider, platform, gameType, pageable);
    return ResponseEntity.ok(new SuccessResponse<>("Ranking 조회 성공", response));
  }

  @GetMapping("/platform-stats")
  public ResponseEntity<SuccessResponse<PlatformUserCountResponse>> getPlatformStats() {
    PlatformUserCountResponse response = rankService.getPlatformUserCounts();
    return ResponseEntity.ok(new SuccessResponse<>("플랫폼별 유저 수 조회 성공", response));
  }
}