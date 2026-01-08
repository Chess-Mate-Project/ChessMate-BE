package com.chessmate.infra_persistence.mapper;

import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.infra_persistence.entity.UserFirstMoveStatEntity;

public class UserFirstMoveStatMapper {

  public static UserFirstMoveStat toDomain(UserFirstMoveStatEntity entity) {
    if (entity == null) return null;

    return UserFirstMoveStat.builder()
        .id(entity.getId())
        .userId(entity.getUserId())
        .firstMove(entity.getFirstMove())
        .build();
  }

  public static UserFirstMoveStatEntity toEntity(UserFirstMoveStat domain) {
    if (domain == null) return null;

    return UserFirstMoveStatEntity.builder()
        .id(domain.getId())
        .userId(domain.getUserId())
        .firstMove(domain.getFirstMove())
        .build();
  }
}

