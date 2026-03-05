package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Lichess Perf API 응답 DTO
 * - /api/user/{username}/perf/{perf} 엔드포인트 응답
 * - 특정 게임 타입의 상세 퍼포먼스 정보 포함
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserPerfDto(
    UserInfoDto user,
    PerfDetailsDto perf,
    Integer rank,
    Double percentile,
    StatDto stat
) {
}

