package com.chessmate.infra_persistence.stat.mapper;

import com.chessmate.domain.stat.UserPerfStat;
import com.chessmate.infra_persistence.stat.entity.UserPerfStatJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserPerfStatMapper {

    public UserPerfStatJpaEntity toEntity(UserPerfStat stat) {
        UserPerfStatJpaEntity entity = UserPerfStatJpaEntity.builder()
            .userId(stat.getUserId())
            .platform(stat.getPlatform())
            .timeClass(stat.getTimeClass())
            .rating(stat.getRating())
            .games(stat.getGames())
            .wins(stat.getWins())
            .losses(stat.getLosses())
            .draws(stat.getDraws())
            .build();
        if (stat.getId() != null) entity.setId(stat.getId());
        return entity;
    }

    public UserPerfStat toDomain(UserPerfStatJpaEntity entity) {
        return UserPerfStat.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .platform(entity.getPlatform())
            .timeClass(entity.getTimeClass())
            .rating(entity.getRating())
            .games(entity.getGames())
            .wins(entity.getWins())
            .losses(entity.getLosses())
            .draws(entity.getDraws())
            .build();
    }
}
