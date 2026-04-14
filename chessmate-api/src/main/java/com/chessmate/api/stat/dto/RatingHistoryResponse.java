package com.chessmate.api.stat.dto;

import java.util.List;

public record RatingHistoryResponse(
    String from,                        // "2025-04" 형식 (1년 전 월)
    String to,                          // "2026-04" 형식 (이번 달)
    List<MonthlyRatingEntry> data
) {}
