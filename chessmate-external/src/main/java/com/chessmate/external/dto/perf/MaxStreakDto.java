package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 최고 스트릭 (게임 수/시간)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MaxStreakDto(
    @JsonProperty("v")
    Integer v,
    StreakPeriodDto from,
    StreakPeriodDto to
) {
}

