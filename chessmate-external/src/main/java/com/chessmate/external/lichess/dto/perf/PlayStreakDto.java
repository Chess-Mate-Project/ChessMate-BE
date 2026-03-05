package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 플레이 스트릭 (게임 수/시간)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayStreakDto(
    PlayStreakValueDto nb,     // 게임 수 스트릭
    PlayStreakValueDto time,   // 플레이 시간 스트릭
    String lastDate            // 마지막 플레이 날짜
) {
}

