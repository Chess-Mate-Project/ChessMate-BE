package com.chessmate.external.api.lichess;

import com.chessmate.external.dto.account.LichessAccountDto;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Lichess Public API 클라이언트
 * Lichess API와 통신하기 위한 HTTP Interface입니다.
 *
 * Base URL: https://lichess.org
 */
@HttpExchange
public interface LichessApi {

  /**
   * 현재 인증된 사용자의 계정 정보 조회 (OAuth Token 필요)
   * Bearer token을 사용하여 현재 로그인한 사용자의 정보를 조회합니다.
   *
   * API Endpoint: GET /api/account
   *
   * @param authHeader Authorization 헤더 (Bearer token)
   *                   예: "Bearer YOUR_ACCESS_TOKEN"
   * @return 현재 사용자의 계정 정보 (ID, username, rating, 플레이 통계 등)
   */
  @GetExchange("/api/account")
  LichessAccountDto getCurrentAccount(
      @RequestHeader("Authorization") String authHeader
  );

}