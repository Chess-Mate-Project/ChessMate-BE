package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 게임 타입별 퍼포먼스 상세 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfDetailsDto(
    GlickoDto glicko,
    Integer nb,           // 게임 수
    Integer progress      // 최근 진행도
) {
}

