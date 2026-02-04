package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 게임 결과 (최고/최저 스코어)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameResultDto(
    Integer opRating,
    OpponentDto opId,
    String at,              // ISO 8601 날짜
    String gameId
) {
}

