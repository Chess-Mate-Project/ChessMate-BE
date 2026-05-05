package com.chessmate.infra_persistence.stat.mapper;

import com.chessmate.domain.stat.UserMonthlyRatingStat;
import com.chessmate.infra_persistence.stat.entity.UserMonthlyRatingStatJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMonthlyRatingStatMapper {

    public UserMonthlyRatingStatJpaEntity toEntity(UserMonthlyRatingStat domain) {
        return UserMonthlyRatingStatJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .platform(domain.getPlatform())
            .timeClass(domain.getTimeClass())
            .year(domain.getYear())
            .month(domain.getMonth())
            .rating(domain.getRating())
            .build();
    }

    public UserMonthlyRatingStat toDomain(UserMonthlyRatingStatJpaEntity entity) {
        return UserMonthlyRatingStat.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .platform(entity.getPlatform())
            .timeClass(entity.getTimeClass())
            .year(entity.getYear())
            .month(entity.getMonth())
            .rating(entity.getRating())
            .build();
    }
}