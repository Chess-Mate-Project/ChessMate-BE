package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 결과 스트릭 정보 (승리/패배)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResultStreaksDto(
    ResultStreakDto win,
    ResultStreakDto loss
) {
}

