package com.chessmate.external.dto.chesscom;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chess.com 게임 아카이브 응답 DTO
 * 사용자의 월별 게임 아카이브 URL 목록을 담습니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChesscomGameArchivesResponse {

  /**
   * 게임 아카이브 URL 목록
   * 예: https://api.chess.com/pub/player/hikaru/games/2014/01
   */
  @JsonProperty("archives")
  private List<String> archives;
}

