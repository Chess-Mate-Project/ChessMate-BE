package com.chessmate.domain.sync;

import com.chessmate.common.dto.OAuthPlatForm;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncJob {

    private Long id;
    private Long userId;
    private OAuthPlatForm platform;
    private String platformUsername;
    private SyncStatus status;
    private String syncCursor;
    private int totalFetched;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // AtomicInteger for thread-safe count updates (Chess.com 병렬 처리)
    private final AtomicInteger atomicFetched = new AtomicInteger(0);

    public static SyncJob create(Long userId, OAuthPlatForm platform, String platformUsername) {
        SyncJob job = new SyncJob();
        job.userId = userId;
        job.platform = platform;
        job.platformUsername = platformUsername;
        job.status = SyncStatus.PENDING;
        job.totalFetched = 0;
        job.createdAt = LocalDateTime.now();
        job.updatedAt = LocalDateTime.now();
        return job;
    }

    /**
     * 증분 수집용 팩토리. cursor를 미리 지정해 Worker가 해당 지점 이후부터만 수집합니다.
     * Chess.com: cursor = "yyyy/MM" (전월), filterPending이 당월 아카이브만 반환하도록 유도.
     */
    public static SyncJob createWithCursor(Long userId, OAuthPlatForm platform, String platformUsername, String cursor) {
        SyncJob job = create(userId, platform, platformUsername);
        job.syncCursor = cursor;
        return job;
    }

    public void start() {
        this.status = SyncStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = SyncStatus.COMPLETED;
        this.totalFetched = this.atomicFetched.get();
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String errorMsg) {
        this.status = SyncStatus.FAILED;
        this.errorMsg = errorMsg != null && errorMsg.length() > 500
            ? errorMsg.substring(0, 500)
            : errorMsg;
        this.updatedAt = LocalDateTime.now();
    }

    public void expireToken() {
        this.status = SyncStatus.TOKEN_EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /** Lichess 순차 처리용: cursor + count 업데이트 */
    public void progress(String cursor, int count) {
        this.syncCursor = cursor;
        this.totalFetched += count;
        this.updatedAt = LocalDateTime.now();
    }

    /** Chess.com 병렬 처리용: thread-safe cursor + count 업데이트 */
    public synchronized void progressAtomic(String cursor, int count) {
        this.syncCursor = cursor;
        this.atomicFetched.addAndGet(count);
        this.updatedAt = LocalDateTime.now();
    }

    // ======================== Getters / Setters ========================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }

    public OAuthPlatForm getPlatform() { return platform; }

    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }

    public String getSyncCursor() { return syncCursor; }
    public void setSyncCursor(String syncCursor) { this.syncCursor = syncCursor; }

    public int getTotalFetched() { return totalFetched; }
    public void setTotalFetched(int totalFetched) {
        this.totalFetched = totalFetched;
        this.atomicFetched.set(totalFetched);
    }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public void setUserId(Long userId) { this.userId = userId; }

    public void setPlatform(OAuthPlatForm platform) { this.platform = platform; }

    public String getPlatformUsername() { return platformUsername; }
    public void setPlatformUsername(String platformUsername) { this.platformUsername = platformUsername; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}