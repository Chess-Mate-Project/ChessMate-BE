package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 현재/최고 스트릭 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StreakValueDto(
    @JsonProperty("v")
    Integer v,                    // 스트릭 수
    StreakPeriodDto from,
    StreakPeriodDto to
) {
}

