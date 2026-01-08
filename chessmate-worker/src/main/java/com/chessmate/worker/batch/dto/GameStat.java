package com.chessmate.worker.batch.dto;


import com.chessmate.domain.userColorStat.UserColorStat;
import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import lombok.Builder;

@Builder
public record GameStat(

  UserDailyStreak dailyStreak,
  UserColorStat colorStat,
  UserFirstMoveStat firstMoveStat
  ){
}