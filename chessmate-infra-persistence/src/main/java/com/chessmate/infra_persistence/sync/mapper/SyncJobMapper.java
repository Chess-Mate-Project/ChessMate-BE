package com.chessmate.infra_persistence.sync.mapper;

import com.chessmate.domain.sync.SyncJob;
import com.chessmate.infra_persistence.sync.entity.SyncJobJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class SyncJobMapper {

    public SyncJobJpaEntity toEntity(SyncJob domain) {
        return SyncJobJpaEntity.builder()
            .id(domain.getId())
            .userId(domain.getUserId())
            .platform(domain.getPlatform())
            .platformUsername(domain.getPlatformUsername())
            .status(domain.getStatus())
            .syncCursor(domain.getSyncCursor())
            .totalFetched(domain.getTotalFetched())
            .errorMsg(domain.getErrorMsg())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .build();
    }

    public SyncJob toDomain(SyncJobJpaEntity entity) {
        SyncJob job = new SyncJob();
        job.setId(entity.getId());
        job.setUserId(entity.getUserId());
        job.setPlatform(entity.getPlatform());
        job.setPlatformUsername(entity.getPlatformUsername());
        job.setStatus(entity.getStatus());
        job.setSyncCursor(entity.getSyncCursor());
        job.setTotalFetched(entity.getTotalFetched());
        job.setErrorMsg(entity.getErrorMsg());
        job.setCreatedAt(entity.getCreatedAt());
        job.setUpdatedAt(entity.getUpdatedAt());
        return job;
    }
}