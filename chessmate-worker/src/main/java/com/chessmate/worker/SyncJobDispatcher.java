package com.chessmate.worker;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.infra_redis.sync.SyncJobConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SyncJob 디스패처.
 *
 * 1초마다 플랫폼별 Redis 큐를 폴링하여 SyncJob을 꺼내고,
 * 해당 플랫폼 Worker에게 처리를 위임합니다.
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncJobDispatcher {

    private final SyncJobConsumer consumer;
    private final SyncJobRepository syncJobRepository;
    private final LichessGameSyncWorker lichessWorker;
    private final ChessComGameSyncWorker chessComWorker;

    @Scheduled(fixedDelay = 1000)
    public void pollLichess() {
        consumer.poll(OAuthPlatForm.LICHESS)
            .flatMap(syncJobRepository::findById)
            .ifPresent(job -> {
                log.info("[Dispatcher] Lichess job 처리 시작 jobId={}", job.getId());
                lichessWorker.process(job);
            });
    }

    @Scheduled(fixedDelay = 1000)
    public void pollChessCom() {
        consumer.poll(OAuthPlatForm.CHESSCOM)
            .flatMap(syncJobRepository::findById)
            .ifPresent(job -> {
                log.info("[Dispatcher] Chess.com job 처리 시작 jobId={}", job.getId());
                chessComWorker.process(job);
            });
    }
}