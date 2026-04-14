package com.chessmate.api.stat.dto;

import com.chessmate.domain.stat.UserDailyGameStat;
import java.time.LocalDate;

public record DailyGameStatResponse(
    LocalDate date,
    int total,
    int wins,
    int draws,
    int losses
) {
    public static DailyGameStatResponse from(UserDailyGameStat stat) {
        return new DailyGameStatResponse(
            stat.getDate(),
            stat.getTotal(),
            stat.getWins(),
            stat.getDraws(),
            stat.getLosses()
        );
    }
}
