package com.chessmate.infra_persistence.stat.mapper;

import com.chessmate.domain.stat.UserDailyGameStat;
import com.chessmate.infra_persistence.stat.entity.UserDailyGameStatJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserDailyGameStatMapper {

    public UserDailyGameStatJpaEntity toEntity(UserDailyGameStat domain) {
        return UserDailyGameStatJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .platform(domain.getPlatform())
            .date(domain.getDate())
            .total(domain.getTotal())
            .wins(domain.getWins())
            .draws(domain.getDraws())
            .losses(domain.getLosses())
            .build();
    }

    public UserDailyGameStat toDomain(UserDailyGameStatJpaEntity entity) {
        return UserDailyGameStat.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .platform(entity.getPlatform())
            .date(entity.getDate())
            .total(entity.getTotal())
            .wins(entity.getWins())
            .draws(entity.getDraws())
            .losses(entity.getLosses())
            .build();
    }
}