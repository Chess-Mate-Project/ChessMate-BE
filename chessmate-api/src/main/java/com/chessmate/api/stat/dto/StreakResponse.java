package com.chessmate.api.stat.dto;

import java.util.List;

public record StreakResponse(
    int currentStreak,
    List<YearlyGameStatResponse> years
) {}