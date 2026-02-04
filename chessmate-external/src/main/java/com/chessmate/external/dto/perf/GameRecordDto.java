package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 게임 기록 (최고/최저 레이팅, 우승/패배 기록)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameRecordDto(
    @JsonProperty("int")
    Integer int_,           // 레이팅
    String at,              // ISO 8601 날짜
    String gameId
) {
}


