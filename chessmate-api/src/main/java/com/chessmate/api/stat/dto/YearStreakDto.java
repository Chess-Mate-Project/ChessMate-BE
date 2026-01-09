package com.chessmate.api.stat.dto;

import java.time.Year;
import java.util.List;

public record YearStreakDto (
    Year year,
    List<DailyStreakDto> dailyStreakDto
    ){}