package com.chessmate.infra_persistence.stat.repositoryImpl;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserMonthlyRatingStat;
import com.chessmate.domain.stat.UserMonthlyRatingStatRepository;
import com.chessmate.infra_persistence.stat.jpaRepository.UserMonthlyRatingStatJpaRepository;
import com.chessmate.infra_persistence.stat.mapper.UserMonthlyRatingStatMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserMonthlyRatingStatRepositoryImpl implements UserMonthlyRatingStatRepository {

    private final UserMonthlyRatingStatJpaRepository jpaRepository;
    private final UserMonthlyRatingStatMapper mapper;

    @Override
    public List<UserMonthlyRatingStat> saveAll(List<UserMonthlyRatingStat> stats) {
        var entities = stats.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        jpaRepository.deleteByUserIdAndPlatform(userId, platform);
    }

    @Override
    public List<UserMonthlyRatingStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findByUserIdAndPlatform(userId, platform).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<UserMonthlyRatingStat> findByUserIdAndPlatformAndTimeClassAndYearAndMonth(
        Long userId, OAuthPlatForm platform, String timeClass, int year, int month) {
        return jpaRepository.findByUserIdAndPlatformAndTimeClassAndYearAndMonth(userId, platform, timeClass, year, month)
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.existsByUserIdAndPlatform(userId, platform);
    }
}