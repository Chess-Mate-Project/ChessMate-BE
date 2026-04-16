package com.chessmate.infra_persistence.sync.entity;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.sync.SyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "sync_job",
    indexes = {
        @Index(name = "idx_user_platform", columnList = "user_id, platform"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncJobJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private OAuthPlatForm platform;

    @Column(name = "platform_username", nullable = false, length = 100)
    private String platformUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncStatus status;

    /**
     * Lichess  : 마지막 gameId
     * Chess.com: 마지막 완료 월 "2024/03"
     */
    @Column(name = "sync_cursor", length = 255)
    private String syncCursor;

    @Column(name = "total_fetched", nullable = false)
    private int totalFetched;

    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}