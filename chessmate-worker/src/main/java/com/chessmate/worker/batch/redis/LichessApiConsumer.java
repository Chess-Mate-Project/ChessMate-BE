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

  private final LichessApiRedisService lichessApiRedisService;
  private final LichessApiTaskHandler lichessApiTaskHandler;

  // Exponential backoff configuration
  private static final long INITIAL_BACKOFF_MS = 1000; // 1 second
  private static final long MAX_BACKOFF_MS = 60000; // 60 seconds
  private static final int MAX_CONSECUTIVE_FAILURES = 10;
  private static final long CIRCUIT_BREAKER_WAIT_MS = 300000; // 5 minutes

  private int consecutiveFailures = 0;
  private long currentBackoffMs = INITIAL_BACKOFF_MS;

  @Override
  public void run(String... args) {
    Thread workerThread = new Thread(() -> {
      log.info("Lichess Worker Consumer 시작됨...");

      while (!Thread.currentThread().isInterrupted()) {
        long startTime = System.currentTimeMillis();

        try {
          // Check if circuit breaker is open (too many consecutive failures)
          if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("Circuit breaker opened after {} consecutive failures. Waiting {} ms before retry...",
                consecutiveFailures, CIRCUIT_BREAKER_WAIT_MS);
            Thread.sleep(CIRCUIT_BREAKER_WAIT_MS);
            consecutiveFailures = 0; // Reset after circuit breaker wait
            currentBackoffMs = INITIAL_BACKOFF_MS; // Reset backoff
            continue;
          }

          // CacheService에 추가한 brPopMultiple 활용 (high 큐 먼저 확인)
          LichessApiTask task = lichessApiRedisService.popGameTask();

          if (task != null) {
            lichessApiTaskHandler.handle(task);
            // Reset failure counters on successful processing
            consecutiveFailures = 0;
            currentBackoffMs = INITIAL_BACKOFF_MS;
          }
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.info("Worker Consumer interrupted, shutting down...");
          break;
        } catch (Exception e) {
          consecutiveFailures++;
          log.error("Worker Consumer 루프 에러 (consecutive failures: {}): {}",
              consecutiveFailures, e.getMessage(), e);

          try {
            long duration = System.currentTimeMillis() - startTime;
            // Apply exponential backoff on errors
            long sleepTime = Math.max(0, currentBackoffMs - duration);

            if (sleepTime > 0) {
              log.warn("Backing off for {} ms due to error (backoff level: {} ms)",
                  sleepTime, currentBackoffMs);
              Thread.sleep(sleepTime);
            }

            // Increase backoff for next error (exponential backoff)
            currentBackoffMs = Math.min(currentBackoffMs * 2, MAX_BACKOFF_MS);

          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.info("Worker Consumer interrupted during backoff, shutting down...");
            break;
          }
        }
      }
    });

    workerThread.setName("Worker-Consumer-1");
    workerThread.setDaemon(true);
    workerThread.start();
  }
}