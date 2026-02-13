package com.chessmate.infra_redis.redis;

import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.CacheProperties.Redis;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class LichessApiRedisService {
  private static final String QUEUE_FAST = "task:queue:fast";
  private static final String QUEUE_HEAVY = "task:queue:low";

  private final RedisService redisService;

  public void pushTask(LichessApiTask task) {
    String queueKey;

    //작업 타입에 따른 큐 분리
    if (task.type() == TaskType.GAMES) {
      redisService.leftPush(QUEUE_HEAVY, task);
    } else {
      redisService.leftPush(QUEUE_FAST, task);
    }
  }

  public LichessApiTask popGameTask() {
    return redisService.brPopMultiple(5, LichessApiTask.class, QUEUE_FAST, QUEUE_HEAVY);
  }
}
