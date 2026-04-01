package com.chessmate.worker;

import com.chessmate.common.dto.GameSyncTaskDto;
import com.chessmate.infra_redis.redis.QueueKey;
import com.chessmate.infra_redis.redis.RedisService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameTaskWorker implements CommandLineRunner {
  private final RedisService redisService;
  private final GameTaskProcessor gameTaskProcessor;


  @Override
  public void run(String... args) throws Exception {
      Thread workThread = new Thread(() -> {
        log.info("queue 동기화 루프 쓰레드 시작");

        while(true) {
          GameSyncTaskDto task = redisService.dequeue(
              QueueKey.GAME_SYNC_TASKS.getKey(),
              GameSyncTaskDto.class,
              30,
              TimeUnit.SECONDS
          );

          if (task != null) {
            log.info("📦 작업 발견! taskId: {}", task.getTaskId());
            gameTaskProcessor.process(task); // 실제 비즈니스 로직 실행
          }
        }
      });
  }
}
