package com.chessmate.domain.sync;

import com.chessmate.common.dto.OAuthPlatForm;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface SyncJobRepository {

    SyncJob save(SyncJob syncJob);

    Optional<SyncJob> findById(Long id);

    Optional<SyncJob> findLatestByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    // updatedAt 기준으로 activeAfter 이후에 활동한 PENDING/IN_PROGRESS 잡의 userId Set
    Set<Long> findActiveUserIdsByPlatform(OAuthPlatForm platform, LocalDateTime activeAfter);

    // 플랫폼 전체 유저의 최신 잡 status 한 번에 조회 (userId → SyncStatus)
    Map<Long, SyncStatus> findLatestStatusByPlatform(OAuthPlatForm platform);
}