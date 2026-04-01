package com.chessmate.worker.batch.dto;


import lombok.Builder;

@Builder
public record GameStat(

  UserDailyStreak dailyStreak,
  UserColorStat colorStat,
  UserFirstMoveStat firstMoveStat
  ){
}