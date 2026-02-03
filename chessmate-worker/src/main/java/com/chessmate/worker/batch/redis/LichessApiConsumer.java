package com.chessmate.worker.batch.redis;

import com.chessmate.infra_redis.redis.LichessApiRedisService;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LichessApiConsumer implements CommandLineRunner {

  private final LichessApiRedisService lichessApiService;
  private final LichessApiTaskHandler lichessApiTaskHandler;

  @Override
  public void run(String... args) {
    Thread workerThread = new Thread(() -> {
      log.info("Lichess Worker Consumer 시작됨...");

      while (!Thread.currentThread().isInterrupted()) {
        long startTime = System.currentTimeMillis();

        try {
          // CacheService에 추가한 brPopMultiple 활용 (high 큐 먼저 확인)
          LichessApiTask task = lichessApiService.popGameTask();

          if (task != null) {
            lichessApiTaskHandler.handle(task);
          }
        } catch (Exception e) {


          log.error("Worker Consumer 루프 에러: {}", e.getMessage());
          try {

            long duration = System.currentTimeMillis() - startTime;
            long sleepTime = Math.max(0, 5000 - duration);


            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
          }



        }

      }
    });

    workerThread.setName("Worker-Consumer-1");
    workerThread.setDaemon(true); // 애플리케이션 종료 시 함께 종료
    workerThread.start();
  }
}