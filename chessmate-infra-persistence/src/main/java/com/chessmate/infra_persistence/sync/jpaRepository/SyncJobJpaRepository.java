package com.chessmate.infra_persistence.sync.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.sync.SyncStatus;
import com.chessmate.infra_persistence.sync.entity.SyncJobJpaEntity;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobJpaRepository extends JpaRepository<SyncJobJpaEntity, Long> {

    Optional<SyncJobJpaEntity> findFirstByUserIdAndPlatformOrderByCreatedAtDesc(
        Long userId,
        OAuthPlatForm platform
    );

    boolean existsByUserIdAndPlatformAndStatusIn(
        Long userId,
        OAuthPlatForm platform,
        Collection<SyncStatus> statuses
    );
}