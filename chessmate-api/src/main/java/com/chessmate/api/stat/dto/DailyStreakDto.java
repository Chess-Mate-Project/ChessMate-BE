package com.chessmate.api.stat.dto;

import java.time.LocalDate;

public record DailyStreakDto (
  LocalDate date,
  int win,
  int lose,
  int draw,
  int total
) {}
