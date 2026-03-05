package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 현재 스트릭 (게임 수/시간)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentStreakDto(
    @JsonProperty("v")
    Integer v
) {
}

