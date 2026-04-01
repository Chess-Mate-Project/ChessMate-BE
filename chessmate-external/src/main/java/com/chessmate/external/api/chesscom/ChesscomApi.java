package com.chessmate.external.api.chesscom;

import com.chessmate.external.dto.ChesscomMonthlyArchiveResponse;
import com.chessmate.external.dto.chesscom.ChesscomGameArchivesResponse;
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
   *         예: {
   *           "archives": [
   *             "https://api.chess.com/pub/player/hikaru/games/2014/01",
   *             "https://api.chess.com/pub/player/hikaru/games/2014/02",
   *             ...
   *           ]
   *         }
   */
  @GetExchange("/pub/player/{username}/games/archives")
  ChesscomGameArchivesResponse getGameArchives(String username);

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
   * 전체 URL을 이용한 게임 아카이브 조회
   * 아카이브 URL 전체가 주어졌을 때 해당 월의 모든 게임 정보를 조회합니다.
   * Chess.com API에서 반환된 아카이브 URL을 그대로 사용할 수 있습니다.
   *
   * @param archiveUrl 전체 아카이브 URL
   *                   예: https://api.chess.com/pub/player/hikaru/games/2014/01
   * @return 해당 월의 모든 게임 정보를 포함하는 응답
   */
  @GetExchange
  ChesscomMonthlyArchiveResponse getArchiveByUrl(String archiveUrl);
}


