package com.chessmate.domain.game;

public record MonthlyRating(
    String timeClass,
    int year,
    int month,
    int rating
) {}