package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Glicko 레이팅 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GlickoDto(
    Double rating,
    Double deviation,
    Boolean provisional
) {
}

