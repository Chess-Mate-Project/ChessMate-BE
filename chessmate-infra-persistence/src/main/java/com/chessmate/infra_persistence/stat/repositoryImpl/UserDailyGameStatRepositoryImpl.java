package com.chessmate.infra_persistence.stat.repositoryImpl;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserDailyGameStat;
import com.chessmate.domain.stat.UserDailyGameStatRepository;
import com.chessmate.infra_persistence.stat.jpaRepository.UserDailyGameStatJpaRepository;
import com.chessmate.infra_persistence.stat.mapper.UserDailyGameStatMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserDailyGameStatRepositoryImpl implements UserDailyGameStatRepository {

    private final UserDailyGameStatJpaRepository jpaRepository;
    private final UserDailyGameStatMapper mapper;

    @Override
    @Transactional
    public void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        jpaRepository.deleteByUserIdAndPlatform(userId, platform);
    }

    @Override
    public List<UserDailyGameStat> saveAll(List<UserDailyGameStat> stats) {
        var entities = stats.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserDailyGameStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findByUserIdAndPlatform(userId, platform).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserDailyGameStat> findByUserIdAndPlatformAndYear(Long userId, OAuthPlatForm platform, int year) {
        return jpaRepository.findByUserIdAndPlatformAndYear(userId, platform, year).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }
}