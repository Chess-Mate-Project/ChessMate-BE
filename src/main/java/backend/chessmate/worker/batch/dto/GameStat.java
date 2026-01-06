package backend.chessmate.worker.batch.dto;

import backend.chessmate.api.user.entity.UserColorStat;
import backend.chessmate.api.user.entity.UserDailyStreak;
import backend.chessmate.api.user.entity.UserFirstMoveStat;
import lombok.Builder;

@Builder
public record GameStat(

  UserDailyStreak dailyStreak,
  UserColorStat colorStat,
  UserFirstMoveStat firstMoveStat
  ){
}