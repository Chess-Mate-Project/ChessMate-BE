package com.chessmate.domain.sync;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.Optional;

public interface SyncJobRepository {

    SyncJob save(SyncJob syncJob);

    Optional<SyncJob> findById(Long id);

    Optional<SyncJob> findLatestByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}