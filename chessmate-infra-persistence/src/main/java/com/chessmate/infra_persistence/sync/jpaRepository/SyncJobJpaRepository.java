package com.chessmate.infra_persistence.sync.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.sync.entity.SyncJobJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncJobJpaRepository extends JpaRepository<SyncJobJpaEntity, Long> {

    @Query("SELECT s FROM SyncJobJpaEntity s WHERE s.userId = :userId AND s.platform = :platform ORDER BY s.createdAt DESC LIMIT 1")
    Optional<SyncJobJpaEntity> findLatestByUserIdAndPlatform(
        @Param("userId") Long userId,
        @Param("platform") OAuthPlatForm platform
    );
}