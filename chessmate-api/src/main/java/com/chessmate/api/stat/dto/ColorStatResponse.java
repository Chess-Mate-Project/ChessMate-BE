package com.chessmate.api.stat.dto;

import com.chessmate.domain.stat.UserColorStat;

public record ColorStatResponse(
    String timeClass,
    String color,
    int total,
    int wins,
    int draws,
    int losses
) {
    public static ColorStatResponse from(UserColorStat stat) {
        return new ColorStatResponse(
            stat.getTimeClass(),
            stat.getColor(),
            stat.getTotal(),
            stat.getWins(),
            stat.getDraws(),
            stat.getLosses()
        );
    }
}
