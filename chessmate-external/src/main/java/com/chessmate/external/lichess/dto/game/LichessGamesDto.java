package com.chessmate.external.lichess.dto.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lichess 게임 목록 DTO
 *
 * - Lichess API에서 반환되는 게임 목록 데이터를 담는 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LichessGamesDto (

  String id,

  boolean rated,

  String variant,

  String perf,

  long createdAt,

  long lastMoveAt,

  String status,

  String winner,

  String moves,

  Players players
      ){
}
