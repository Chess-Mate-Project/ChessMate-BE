package com.chessmate.external.lichess.dto.perf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 통계 정보
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StatDto(
    GameRecordDto highest,
    GameRecordDto lowest,
    GameResultsDto bestWins,
    GameResultsDto worstLosses,
    CountDto count,
    ResultStreaksDto resultStreak,
    PlayStreakDto playStreak
) {
}

