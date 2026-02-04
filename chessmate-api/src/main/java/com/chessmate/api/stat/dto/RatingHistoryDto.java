package com.chessmate.api.stat.dto;

import java.time.LocalDate;

public record RatingHistoryDto(
    LocalDate date,
    int lastRating
) {}

