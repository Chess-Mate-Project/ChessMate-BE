package com.chessmate.external.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 게임 참여 구간 정보 (시작/종료)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StreakPeriodDto(
    String at,              // ISO 8601 날짜
    String gameId
) {
}

