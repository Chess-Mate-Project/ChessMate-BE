package com.chessmate.infra_persistence.stat.repositoryImpl;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserPerfStat;
import com.chessmate.domain.stat.UserPerfStatRepository;
import com.chessmate.infra_persistence.stat.jpaRepository.UserPerfStatJpaRepository;
import com.chessmate.infra_persistence.stat.mapper.UserPerfStatMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserPerfStatRepositoryImpl implements UserPerfStatRepository {

    private final UserPerfStatJpaRepository jpaRepository;
    private final UserPerfStatMapper mapper;

    @Override
    public List<UserPerfStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findByUserIdAndPlatform(userId, platform).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public List<UserPerfStat> saveAll(List<UserPerfStat> stats) {
        return jpaRepository.saveAll(stats.stream().map(mapper::toEntity).toList()).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        jpaRepository.deleteByUserIdAndPlatform(userId, platform);
    }

    @Override
    public Optional<UserPerfStat> findTopRatingByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findTopRatingByUserIdAndPlatform(userId, platform)
            .map(mapper::toDomain);
    }

    @Override
    public Map<Long, UserPerfStat> findTopRatingByUserIdsAndPlatform(List<Long> userIds, OAuthPlatForm platform) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.findByUserIdInAndPlatform(userIds, platform).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toMap(
                UserPerfStat::getUserId,
                stat -> stat,
                (a, b) -> a.getRating() >= b.getRating() ? a : b
            ));
    }
}
