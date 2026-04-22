package com.chessmate.external.api.lichess;

import com.chessmate.external.dto.account.LichessAccountDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

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
   *
   * API Endpoint: GET /api/account
   */
  @GetExchange("/api/account")
  LichessAccountDto getCurrentAccount(
      @RequestHeader("Authorization") String authHeader
  );

  /**
   * 특정 사용자의 게임 목록 조회 (NDJSON 스트리밍)
   *
   * API Endpoint: GET /api/games/user/{username}
   *
   * @param authHeader Authorization 헤더 (Bearer token)
   * @param accept     Accept 헤더 — "application/x-ndjson" 고정
   * @param username   Lichess 사용자명
   * @param max        최대 게임 수 (최대 100)
   * @param until      이 gameId 이전 게임만 조회 (커서 페이징)
   * @param sort       정렬 방향 — "dateDesc" 고정
   * @return 줄바꿈(\n) 구분 NDJSON 문자열
   */
  /**
   * 특정 사용자의 공개 프로필 조회 (인증 불필요)
   * perfs(타임클래스별 레이팅) 포함
   *
   * API Endpoint: GET /api/user/{username}
   */
  @GetExchange("/api/user/{username}")
  LichessAccountDto getUser(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @PathVariable("username") String username
  );

  /**
   * 특정 사용자의 게임 목록 조회.
   *
   * 전체 수집 (최초): until=gameId, since=null, sort=dateDesc
   * 증분 수집 (정기): until=null, since=epochMillis, sort=dateAsc
   *
   * Lichess API Rate Limit: 20 req/sec (인증), 비인증 더 낮음
   * Reference: https://lichess.org/api#tag/Games/operation/apiGamesUser (2025-04)
   */
  @GetExchange("/api/games/user/{username}")
  String getGames(
      @RequestHeader(value = "Authorization", required = false) String authHeader,
      @RequestHeader("Accept") String accept,
      @PathVariable("username") String username,
      @RequestParam(value = "max", required = false) Integer max,
      @RequestParam(value = "until", required = false) Long until,
      @RequestParam(value = "since", required = false) Long since,
      @RequestParam(value = "sort", required = false) String sort,
      @RequestParam(value = "rated", required = false) Boolean rated
  );
}