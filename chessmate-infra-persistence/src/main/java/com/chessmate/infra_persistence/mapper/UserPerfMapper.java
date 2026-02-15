package com.chessmate.infra_persistence.mapper;

import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.infra_persistence.entity.UserPerfEntity;

public class UserPerfMapper {

  public static UserPerf toDomain(UserPerfEntity entity) {
    if (entity == null) return null;
    return UserPerf.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .gameType(entity.getGameType())
        .rating(entity.getRating())
        .gamesPlayed(entity.getGamesPlayed())
        .prov(entity.isProv())
        .all(entity.getAll())
        .rated(entity.getRated())
        .wins(entity.getWins())
        .losses(entity.getLosses())
        .draws(entity.getDraws())
        .tour(entity.getTour())
        .berserk(entity.getBerserk())
        .opAvg(entity.getOpAvg())
        .seconds(entity.getSeconds())
        .disconnects(entity.getDisconnects())
        .highestRating(entity.getHighestRating())
        .lowestRating(entity.getLowestRating())
        .maxStreak(entity.getMaxStreak())
        .maxLossStreak(entity.getMaxLossStreak())
        .uncertain(entity.isUncertain())
        .build();
  }

  public static UserPerfEntity toEntity(UserPerf domain) {
    if (domain == null) return null;
    return UserPerfEntity.builder()
        .id(domain.getId())
        .userId(domain.getUserId())
        .gameType(domain.getGameType())
        .rating(domain.getRating())
        .gamesPlayed(domain.getGamesPlayed())
        .prov(domain.isProv())
        .all(domain.getAll())
        .rated(domain.getRated())
        .wins(domain.getWins())
        .losses(domain.getLosses())
        .draws(domain.getDraws())
        .tour(domain.getTour())
        .berserk(domain.getBerserk())
        .opAvg(domain.getOpAvg())
        .seconds(domain.getSeconds())
        .disconnects(domain.getDisconnects())
        .highestRating(domain.getHighestRating())
        .lowestRating(domain.getLowestRating())
        .maxStreak(domain.getMaxStreak())
        .maxLossStreak(domain.getMaxLossStreak())
        .uncertain(domain.isUncertain())
        .build();
  }
}

