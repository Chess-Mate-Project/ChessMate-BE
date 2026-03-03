package com.chessmate.api.lichess.stat.dto;

import java.time.LocalDate;

public record RatingHistoryDto(
    LocalDate date,
    int lastRating
) {}

