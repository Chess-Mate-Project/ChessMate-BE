package com.chessmate.infra_persistence.stat.mapper;

import com.chessmate.domain.stat.UserFirstMoveStat;
import com.chessmate.infra_persistence.stat.entity.UserFirstMoveStatJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserFirstMoveStatMapper {

    public UserFirstMoveStatJpaEntity toEntity(UserFirstMoveStat domain) {
        return UserFirstMoveStatJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .platform(domain.getPlatform())
            .timeClass(domain.getTimeClass())
            .color(domain.getColor())
            .move(domain.getMove())
            .count(domain.getCount())
            .build();
    }

    public UserFirstMoveStat toDomain(UserFirstMoveStatJpaEntity entity) {
        return UserFirstMoveStat.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .platform(entity.getPlatform())
            .timeClass(entity.getTimeClass())
            .color(entity.getColor())
            .move(entity.getMove())
            .count(entity.getCount())
            .build();
    }
}