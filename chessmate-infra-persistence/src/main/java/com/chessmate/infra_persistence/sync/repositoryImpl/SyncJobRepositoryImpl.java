package com.chessmate.infra_persistence.sync.repositoryImpl;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.sync.SyncJob;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.domain.sync.SyncStatus;
import com.chessmate.infra_persistence.sync.jpaRepository.SyncJobJpaRepository;
import com.chessmate.infra_persistence.sync.mapper.SyncJobMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SyncJobRepositoryImpl implements SyncJobRepository {

    private final SyncJobJpaRepository jpaRepository;
    private final SyncJobMapper mapper;

    @Override
    public SyncJob save(SyncJob syncJob) {
        var entity = mapper.toEntity(syncJob);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SyncJob> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SyncJob> findLatestByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.findFirstByUserIdAndPlatformOrderByCreatedAtDesc(userId, platform).map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByUserIdAndPlatform(Long userId, OAuthPlatForm platform) {
        return jpaRepository.existsByUserIdAndPlatformAndStatusIn(
            userId, platform, List.of(SyncStatus.PENDING, SyncStatus.IN_PROGRESS));
    }
}