package com.chessmate.api.stat.dto;

public record MonthlyRatingEntry(
    String yearMonth,   // "2025-04" 형식
    String timeClass,
    int rating
) {}