package com.chessmate.api.stat.dto;

import com.chessmate.domain.stat.UserPerfStat;

public record UserPerfStatResponse(
    String timeClass,
    int rating,
    int games,
    int wins,
    int losses,
    int draws
) {
    public static UserPerfStatResponse from(UserPerfStat stat) {
        return new UserPerfStatResponse(
            stat.getTimeClass(),
            stat.getRating(),
            stat.getGames(),
            stat.getWins(),
            stat.getLosses(),
            stat.getDraws()
        );
    }
}
