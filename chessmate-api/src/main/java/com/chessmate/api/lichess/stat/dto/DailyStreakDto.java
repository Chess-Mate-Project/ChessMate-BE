package com.chessmate.api.lichess.stat.dto;

import java.time.LocalDate;

public record DailyStreakDto (
  LocalDate date,
  int win,
  int lose,
  int draw,
  int total,
  int lastRating // 마지막 게임 후 정산된 레이팅
) {}
