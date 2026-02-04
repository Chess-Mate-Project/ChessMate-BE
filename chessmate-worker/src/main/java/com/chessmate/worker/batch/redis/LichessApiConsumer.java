package com.chessmate.worker.batch.redis;

import com.chessmate.infra_redis.redis.LichessApiRedisService;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

  private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
  private final AtomicLong currentBackoffMs = new AtomicLong(INITIAL_BACKOFF_MS);

  @Override
  public void run(String... args) {
    Thread workerThread = new Thread(() -> {
      log.info("Lichess Worker Consumer 시작됨...");

      while (!Thread.currentThread().isInterrupted()) {
        long startTime = System.currentTimeMillis();

        try {
          // Check if circuit breaker is open (too many consecutive failures)
          if (consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("Circuit breaker opened after {} consecutive failures. Waiting {} ms before retry...",
                consecutiveFailures.get(), CIRCUIT_BREAKER_WAIT_MS);
            try {
              Thread.sleep(CIRCUIT_BREAKER_WAIT_MS);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              log.info("Worker Consumer interrupted during circuit breaker wait, shutting down...");
              break;
            }
            consecutiveFailures.set(0); // Reset after circuit breaker wait
            currentBackoffMs.set(INITIAL_BACKOFF_MS); // Reset backoff
            continue;
          }

          // CacheService에 추가한 brPopMultiple 활용 (high 큐 먼저 확인)
          LichessApiTask task = lichessApiRedisService.popGameTask();

          if (task != null) {
            lichessApiTaskHandler.handle(task);
            // Reset failure counters on successful processing
            consecutiveFailures.set(0);
            currentBackoffMs.set(INITIAL_BACKOFF_MS);
          }
        } catch (Exception e) {
          consecutiveFailures.incrementAndGet();
          log.error("Worker Consumer 루프 에러 (consecutive failures: {}): {}",
              consecutiveFailures.get(), e.getMessage(), e);

          try {
            long duration = System.currentTimeMillis() - startTime;
            // Apply exponential backoff on errors
            long backoff = currentBackoffMs.get();
            long sleepTime = Math.max(0, backoff - duration);

            if (sleepTime > 0) {
              log.warn("Backing off for {} ms due to error (backoff level: {} ms)",
                  sleepTime, backoff);
              Thread.sleep(sleepTime);
            }

            // Increase backoff for next error (exponential backoff)
            currentBackoffMs.updateAndGet(current -> Math.min(current * 2, MAX_BACKOFF_MS));

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