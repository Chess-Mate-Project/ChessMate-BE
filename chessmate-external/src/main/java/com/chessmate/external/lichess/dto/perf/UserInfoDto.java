package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 사용자 기본 정보 (Perf API 응답용)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserInfoDto(
    String name
) {
}


