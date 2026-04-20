package com.chessmate.api.stat.dto;

import com.chessmate.domain.stat.UserFirstMoveStat;

public record FirstMoveStatResponse(
    String timeClass,
    String color,
    String move,
    int count
) {
    public static FirstMoveStatResponse from(UserFirstMoveStat stat) {
        return new FirstMoveStatResponse(
            stat.getTimeClass(),
            stat.getColor(),
            stat.getMove(),
            stat.getCount()
        );
    }
}
