package com.chessmate.infra_persistence.stat.repositoryImpl;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.stat.UserFirstMoveStat;
import com.chessmate.domain.stat.UserFirstMoveStatRepository;
import com.chessmate.infra_persistence.stat.jpaRepository.UserFirstMoveStatJpaRepository;
import com.chessmate.infra_persistence.stat.mapper.UserFirstMoveStatMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserFirstMoveStatRepositoryImpl implements UserFirstMoveStatRepository {

    private final UserFirstMoveStatJpaRepository jpaRepository;
    private final UserFirstMoveStatMapper mapper;

    @Override
    @Transactional
    public void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        jpaRepository.deleteByUserIdAndPlatform(userId, platform);
    }

    @Override
    public List<UserFirstMoveStat> saveAll(List<UserFirstMoveStat> stats) {
        var entities = stats.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserFirstMoveStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findByUserIdAndPlatform(userId, platform).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<UserFirstMoveStat> findByUserIdAndPlatformAndTimeClassAndColorAndMove(Long userId, OAuthPlatForm platform, String timeClass, String color, String move) {
        return jpaRepository.findByUserIdAndPlatformAndTimeClassAndColorAndMove(userId, platform, timeClass, color, move)
            .map(mapper::toDomain);
    }

    @Override
    public List<UserFirstMoveStat> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass) {
        return jpaRepository.findByUserIdAndPlatformAndTimeClass(userId, platform, timeClass).stream()
            .map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.existsByUserIdAndPlatform(userId, platform);
    }
}