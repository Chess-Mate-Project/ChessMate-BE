package com.chessmate.api.lichess.rank.controller;

import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.lichess.rank.dto.RankingResponse;
import com.chessmate.api.lichess.rank.service.RankService;
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

  /**
   * 게임 타입별 랭킹 조회 (페이지네이션)
   *
   * 페이지 크기: 20명 (기본값, size 파라미터로 변경 가능)
   * 캐시 전략: Cache-Aside (1시간 TTL)
   *
   * @param userPrincipal 인증된 사용자 (Optional)
   * @param gameType 게임 타입 (기본값: RAPID)
   * @param pageable 페이지 정보 (page=0, size=20 기본값)
   * @return 랭킹 정보 (내 순위 + 페이지별 랭킹)
   */
  @GetMapping("/ranking")
  public ResponseEntity<SuccessResponse<RankingResponse>> getRanking(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      @RequestParam(defaultValue = "RAPID") GameType gameType,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    log.info("[Ranking API] gameType={}, page={}, size={}, isAuthenticated={}",
        gameType, pageable.getPageNumber(), pageable.getPageSize(), userPrincipal != null);

    RankingResponse response = rankService.getRankers(
        userPrincipal != null ? userPrincipal.getUser() : null,
        gameType,
        pageable
    );

    log.info("[Ranking Response] gameType={}, page={}, rankerCount={}, totalPages={}",
        gameType, pageable.getPageNumber(), response.getRanking().size(), response.getTotalPages());

    return ResponseEntity.ok(
        new SuccessResponse<>("Ranking 조회 성공", response)
    );
  }
}

