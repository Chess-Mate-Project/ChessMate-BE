package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 플레이 스트릭 값 (게임 수/시간)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayStreakValueDto(
    CurrentStreakDto cur,
    MaxStreakDto max
) {
}

