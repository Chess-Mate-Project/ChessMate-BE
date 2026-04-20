package com.chessmate.infra_persistence.game.projection;

public interface MonthlyRatingProjection {
    String getTimeClass();
    int getYear();
    int getMonth();
    int getRating();
}