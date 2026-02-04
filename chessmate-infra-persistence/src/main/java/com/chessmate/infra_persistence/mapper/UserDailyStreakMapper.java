package com.chessmate.infra_persistence.mapper;

import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.infra_persistence.entity.UserDailyStreakEntity;

public class UserDailyStreakMapper {

  public static UserDailyStreak toDomain(UserDailyStreakEntity entity) {
    if (entity == null) return null;
    return UserDailyStreak.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .date(entity.getDate())
        .win(entity.getWin())
        .lose(entity.getLose())
        .draw(entity.getDraw())
        .lastGameAt(entity.getLastGameAt())
        .lastRating(entity.getLastRating())
        .build();
  }

  public static UserDailyStreakEntity toEntity(UserDailyStreak domain) {
    if (domain == null) return null;

    return UserDailyStreakEntity.builder()
        .id(domain.getId())
        .userId(domain.getUserId())
        .date(domain.getDate())
        .win(domain.getWin())
        .lose(domain.getLose())
        .draw(domain.getDraw())
        .lastGameAt(domain.getLastGameAt())
        .lastRating(domain.getLastRating())
        .build();
  }
}

