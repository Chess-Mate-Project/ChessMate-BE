package com.chessmate.infra_persistence.stat.mapper;

import com.chessmate.domain.stat.UserColorStat;
import com.chessmate.infra_persistence.stat.entity.UserColorStatJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserColorStatMapper {

    public UserColorStatJpaEntity toEntity(UserColorStat domain) {
        return UserColorStatJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .platform(domain.getPlatform())
            .timeClass(domain.getTimeClass())
            .color(domain.getColor())
            .wins(domain.getWins())
            .draws(domain.getDraws())
            .losses(domain.getLosses())
            .build();
    }

    public UserColorStat toDomain(UserColorStatJpaEntity entity) {
        return UserColorStat.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .platform(entity.getPlatform())
            .timeClass(entity.getTimeClass())
            .color(entity.getColor())
            .wins(entity.getWins())
            .draws(entity.getDraws())
            .losses(entity.getLosses())
            .build();
    }
}