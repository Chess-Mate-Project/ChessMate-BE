package com.chessmate.infra_redis.sync;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SyncJob ID를 플랫폼별 Redis 큐에 넣는 Producer.
 * OAuth 콜백에서 SyncJob 저장 후 이 클래스를 통해 큐에 등록합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncJobProducer {

    private final RedisService redisService;

    public void enqueue(OAuthPlatForm platform, Long syncJobId) {
        String key = resolveKey(platform);
        redisService.enqueue(key, syncJobId);
        log.info("[SyncJobProducer] enqueued jobId={} to {}", syncJobId, key);
    }

    private String resolveKey(OAuthPlatForm platform) {
        return switch (platform) {
            case LICHESS -> SyncQueueKey.LICHESS.getKey();
            case CHESSCOM -> SyncQueueKey.CHESSCOM.getKey();
        };
    }
}