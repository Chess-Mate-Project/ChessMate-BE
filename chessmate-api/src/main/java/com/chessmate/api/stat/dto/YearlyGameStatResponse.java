package com.chessmate.api.stat.dto;

import java.util.List;

public record YearlyGameStatResponse(
    int year,
    List<DailyGameStatResponse> days
) {}
