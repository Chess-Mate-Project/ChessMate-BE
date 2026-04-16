package com.chessmate.infra_persistence.sync.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.sync.entity.SyncJobJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncJobJpaRepository extends JpaRepository<SyncJobJpaEntity, Long> {

    Optional<SyncJobJpaEntity> findFirstByUserIdAndPlatformOrderByCreatedAtDesc(
        Long userId,
        OAuthPlatForm platform
    );
}