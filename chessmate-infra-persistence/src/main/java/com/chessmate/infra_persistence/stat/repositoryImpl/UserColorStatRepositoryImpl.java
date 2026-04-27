package com.chessmate.infra_persistence.stat.repositoryImpl;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserColorStat;
import com.chessmate.domain.stat.UserColorStatRepository;
import com.chessmate.infra_persistence.stat.jpaRepository.UserColorStatJpaRepository;
import com.chessmate.infra_persistence.stat.mapper.UserColorStatMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserColorStatRepositoryImpl implements UserColorStatRepository {

    private final UserColorStatJpaRepository jpaRepository;
    private final UserColorStatMapper mapper;

    @Override
    @Transactional
    public void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        jpaRepository.deleteByUserIdAndPlatform(userId, platform);
    }

    @Override
    public List<UserColorStat> saveAll(List<UserColorStat> stats) {
        var entities = stats.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserColorStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findByUserIdAndPlatform(userId, platform).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<UserColorStat> findByUserIdAndPlatformAndTimeClassAndColor(Long userId, OAuthPlatForm platform, String timeClass, String color) {
        return jpaRepository.findByUserIdAndPlatformAndTimeClassAndColor(userId, platform, timeClass, color)
            .map(mapper::toDomain);
    }

    @Override
    public List<UserColorStat> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass) {
        return jpaRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.existsByUserIdAndPlatform(userId, platform);
    }
}