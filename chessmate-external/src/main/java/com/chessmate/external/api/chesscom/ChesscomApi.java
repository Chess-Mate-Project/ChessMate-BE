package com.chessmate.external.api.chesscom;

import com.chessmate.external.dto.ChesscomMonthlyArchiveResponse;
import com.chessmate.external.dto.chesscom.ChesscomGameArchivesResponse;
import com.chessmate.external.dto.chesscom.ChesscomPlayerStatsResponse;
import com.chessmate.external.dto.chesscom.ChesscomPublicProfileResponse;
import java.net.URI;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Chess.com Public API 클라이언트
 * 사용자의 게임 데이터, 프로필 등을 조회하는 HTTP Interface입니다.
 *
 * Base URL: https://api.chess.com
 */
@HttpExchange
public interface ChesscomApi {

  /**
   * 특정 사용자의 게임 아카이브 목록 조회
   * 사용자가 플레이한 모든 월별 게임 아카이브의 URL 목록을 반환합니다.
   *
   * API Endpoint: GET /pub/player/{username}/games/archives
   *
   * @param username Chess.com 사용자명
   * @return 게임 아카이브 URL 목록을 포함하는 응답
   * @see ChesscomGameArchivesResponse
   *
   *         예: {
   *           "archives": [
   *             "https://api.chess.com/pub/player/hikaru/games/2014/01",
   *             "https://api.chess.com/pub/player/hikaru/games/2014/02",
   *             ...
   *           ]
   *         }
   */
  @GetExchange("/pub/player/{username}/games/archives")
  ChesscomGameArchivesResponse getGameArchives(@PathVariable("username") String username);

  /**
   * Chess.com 공개 프로필 조회 (가입일 등)
   *
   * API Endpoint: GET /pub/player/{username}
   */
  @GetExchange("/pub/player/{username}")
  ChesscomPublicProfileResponse getPlayerProfile(@PathVariable("username") String username);

  /**
   * 타임클래스별 통계 조회 (레이팅 + 승/무/패)
   *
   * API Endpoint: GET /pub/player/{username}/stats
   */
  @GetExchange("/pub/player/{username}/stats")
  ChesscomPlayerStatsResponse getPlayerStats(@PathVariable("username") String username);

//  /**
//   * 특정 월의 월간 게임 아카이브 조회
//   * 사용자가 특정 연월에 플레이한 모든 완료된 게임의 상세 정보를 반환합니다.
//   *
//   * API Endpoint: GET /pub/player/{username}/games/{YYYY}/{MM}
//   *
//   * @param username Chess.com 사용자명
//   * @param year 연도 (4자리, 예: 2026)
//   * @param month 월 (2자리, 예: 01-12)
//   * @return 해당 월의 모든 게임 정보를 포함하는 응답
//   */
//  @GetExchange("/pub/player/{username}/games/{year}/{month}")
//  ChesscomMonthlyArchiveResponse getMonthlyArchive(
//      String username,
//      String year,
//      String month
//  );

  /**
   * 전체 URL을 이용한 게임 아카이브 조회 (토큰 선택적)
   *
   * @param archiveUrl  전체 아카이브 URL
   * @param authHeader  Authorization 헤더 ("Bearer {token}"). null 가능 — 없으면 Public API 사용
   * @return 해당 월의 모든 게임 정보를 포함하는 응답
   */
  @GetExchange
  ChesscomMonthlyArchiveResponse getArchiveByUrl(
      URI archiveUrl,
      @RequestHeader(value = "Authorization", required = false) String authHeader
  );
}


