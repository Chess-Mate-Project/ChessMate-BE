package com.chessmate.infra_redis.redis;

import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.CacheProperties.Redis;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class LichessApiRedisService {
  private final RedisService redisService;

  public void pushTask(LichessApiTask task) {
    String queueKey;

    //작업 타입에 따른 큐 분리
    if (task.type() == TaskType.GAMES) {
      queueKey = "task:queue:heavy"; // 오래 걸리는 길
    } else {
      queueKey = "task:queue:fast";  // 빨리 처리해야 하는 길
    }

    redisService.leftPush(queueKey, task);
  }

  public LichessApiTask popGameTask() {
    return redisService.brPopMultiple(
        10,
        LichessApiTask.class,
        "task:queue:heavy",
        "task:queue:fast"
    );
  }
}
