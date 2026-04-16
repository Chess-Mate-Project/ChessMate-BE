package com.chessmate.infra_redis.sync;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_redis.redis.RedisService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 플랫폼별 Redis 큐에서 SyncJob ID를 꺼내는 Consumer.
 */
@Component
@RequiredArgsConstructor
public class SyncJobConsumer {

    private final RedisService redisService;

    /**
     * 큐에서 SyncJob ID를 즉시 꺼냅니다 (non-blocking RPOP).
     * 데이터가 없으면 Optional.empty() 반환.
     */
    public Optional<Long> poll(OAuthPlatForm platform) {
        String key = resolveKey(platform);
        Long jobId = redisService.dequeue(key, Long.class);
        return Optional.ofNullable(jobId);
    }

    private String resolveKey(OAuthPlatForm platform) {
        return switch (platform) {
            case LICHESS -> SyncQueueKey.LICHESS.getKey();
            case CHESSCOM -> SyncQueueKey.CHESSCOM.getKey();
        };
    }
}
