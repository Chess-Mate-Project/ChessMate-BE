package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 최고/최저 점수 결과 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameResultsDto(
    List<GameResultDto> results
) {
}

