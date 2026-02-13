package com.chessmate.worker.batch.service;

import com.chessmate.infra_redis.redis.LichessApiRedisService;
import com.chessmate.infra_redis.redis.RedisService;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchBarrierService {

  private static final long TTL_SECONDS = 6 * 60 * 60;

  private final RedisService redisService;
  private final LichessApiRedisService lichessApiRedisService;

  public void initBatch(String batchId, long expected) {
    redisService.save(keyExpected(batchId), expected, TTL_SECONDS);
    redisService.save(keyDone(batchId), 0L, TTL_SECONDS);
    redisService.expire(keyAcks(batchId), TTL_SECONDS);
    redisService.save(keyStatus(batchId), "RUNNING", TTL_SECONDS);
    log.info("[BatchBarrier] init batchId={}, expected={}", batchId, expected);
  }

  public void ackAndMaybeTriggerSnapshot(String batchId, String taskId) {
    if (batchId == null || taskId == null) return;

    // 멱등성: taskId가 이미 ack됐다면 done을 올리지 않음
    Long added = redisService.sAdd(keyAcks(batchId), taskId);
    if (added == null || added == 0L) {
      return;
    }

    Long done = redisService.increment(keyDone(batchId));
    Long expected = redisService.getLong(keyExpected(batchId));
    if (done == null || expected == null) return;

    if (done.equals(expected)) {
      Boolean triggered = redisService.setIfAbsent(keyTriggered(batchId), "1", TTL_SECONDS);
      if (Boolean.TRUE.equals(triggered)) {
        redisService.save(keyStatus(batchId), "SNAPSHOT_ENQUEUED", TTL_SECONDS);
        log.info("[BatchBarrier] complete! enqueue snapshot batchId={}", batchId);

        LichessApiTask snapshotTask = new LichessApiTask(
            null, TaskType.RANKING_SNAPSHOT, null,
            null,
            false,
            batchId,
            "snapshot-" + batchId
        );

        lichessApiRedisService.pushTask(snapshotTask);
      }
    }
  }

  private String keyExpected(String batchId) { return "batch:" + batchId + ":expected"; }
  private String keyDone(String batchId) { return "batch:" + batchId + ":done"; }
  private String keyAcks(String batchId) { return "batch:" + batchId + ":acks"; }
  private String keyTriggered(String batchId) { return "batch:" + batchId + ":triggered"; }
  private String keyStatus(String batchId) { return "batch:" + batchId + ":status"; }
}