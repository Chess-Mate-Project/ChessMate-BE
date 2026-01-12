package com.chessmate.infra_persistence.mapper;

import com.chessmate.domain.userColorStat.UserColorStat;
import com.chessmate.infra_persistence.entity.UserColorStatEntity;

public class UserColorStatMapper {

  public static UserColorStat toDomain(UserColorStatEntity entity) {
    if (entity == null) return null;
    return UserColorStat.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .color(entity.getColor())
        .result(entity.getResult())
        .gameType(entity.getGameType())
        .build();
  }

  public static UserColorStatEntity toEntity(UserColorStat domain) {
    if (domain == null) return null;
    return UserColorStatEntity.builder()
        .id(domain.getId())
        .userId(domain.getUserId())
        .color(domain.getColor())
        .result(domain.getResult())
        .gameType(domain.getGameType())
        .build();
  }
}
