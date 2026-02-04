package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 결과 스트릭 (승리/패배)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResultStreakDto(
    StreakValueDto cur,
    StreakValueDto max
) {
}

