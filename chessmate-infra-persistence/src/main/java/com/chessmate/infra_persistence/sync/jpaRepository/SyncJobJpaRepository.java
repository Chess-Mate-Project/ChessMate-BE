package com.chessmate.infra_persistence.sync.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.sync.SyncStatus;
import com.chessmate.infra_persistence.sync.entity.SyncJobJpaEntity;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncJobJpaRepository extends JpaRepository<SyncJobJpaEntity, Long> {

    Optional<SyncJobJpaEntity> findFirstByUserIdAndPlatformOrderByCreatedAtDesc(
        Long userId,
        OAuthPlatForm platform
    );

    // updatedAt 기준: 마지막으로 상태가 변경된 시각 기준으로 staleness 판단
    @Query("SELECT e.userId FROM SyncJobJpaEntity e " +
           "WHERE e.platform = :platform " +
           "AND e.status IN :statuses " +
           "AND e.updatedAt >= :activeAfter")
    Set<Long> findActiveUserIdsByPlatformAndStatusIn(
        @Param("platform") OAuthPlatForm platform,
        @Param("statuses") Collection<SyncStatus> statuses,
        @Param("activeAfter") LocalDateTime activeAfter
    );

    // 플랫폼 전체 유저의 최신 잡을 한 번에 조회 (userId당 MAX(id) 기준)
    @Query("SELECT e FROM SyncJobJpaEntity e WHERE e.platform = :platform AND e.id IN " +
           "(SELECT MAX(e2.id) FROM SyncJobJpaEntity e2 WHERE e2.platform = :platform GROUP BY e2.userId)")
    List<SyncJobJpaEntity> findLatestJobPerUserByPlatform(@Param("platform") OAuthPlatForm platform);
}