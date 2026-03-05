package com.chessmate.worker.batch.service;


import com.chessmate.common.service.UserBatchService;
import com.chessmate.domain.user.User;
import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.infra_core.repositoryImpl.UserDailyStreakRepositoryImpl;
import com.chessmate.infra_core.repositoryImpl.UserRepositoryImpl;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBatchServiceImpl implements UserBatchService {

  @Qualifier("asyncJobLauncher")
  private final JobLauncher jobLauncher;
  private final Job lichessJob;
  private final UserRepositoryImpl userRepository;
  private final UserDailyStreakRepositoryImpl userDailyStreakRepository;

  @Override
  public void triggerUserUpdate(Long userId, String lichessToken, boolean isFirstTime) {

    log.info("[Batch-Trigger] ========== 배치 작업 트리거 시작 ==========");
    log.info("[Batch-Trigger] userId={}, isFirstTime={}", userId, isFirstTime);

    User user = userRepository.findById(userId).orElseThrow();
    log.info("[Batch-Trigger] 사용자 정보 조회 완료: username={}", user.getUsername());

    Long since = null;
    Long until = System.currentTimeMillis();

    if (!isFirstTime) {
      log.debug("[Batch-Trigger] 마지막 게임 시간 조회 중...");
      Long lastGameAt = userDailyStreakRepository.findLastGameAtByUserId(userId);

      if (lastGameAt != null && lastGameAt > 0) {
        // 마지막 게임의 lastMoveAt + 1ms를 since로 설정하여 중복 조회 방지
        since = lastGameAt + 1;
        log.info("[Batch-Trigger] [SINCE] 마지막 게임: lastGameAt={}, since={} (+1ms 적용 | {}ms 이후 게임 조회)",
            lastGameAt, since, System.currentTimeMillis() - lastGameAt);
        log.info("[Batch-Trigger] [DEBUG] 지난 시간 계산: {} 시간 전",
            (System.currentTimeMillis() - lastGameAt) / (1000.0 * 60 * 60));
      } else {
        log.warn("[Batch-Trigger] [SINCE-NULL] 마지막 게임 기록 없음 - 전체 게임 조회 예정");
        since = 0L;
      }
    } else {
      log.info("[Batch-Trigger] [FIRST-TIME] 첫 로그인 - 전체 게임 조회");
      since = 0L;
    }

    try {
      // 고유한 배치 ID 생성 (UUID)
      String batchId = UUID.randomUUID().toString();

      JobParametersBuilder paramsBuilder = new JobParametersBuilder()
          .addString("username", user.getUsername())
          .addString("token", lichessToken)
          .addString("batchId", batchId)
          .addLong("since", since)
          .addLong("until", until);

      log.info("[Batch-Trigger] [PARAM] since={}, until={} (범위: {} ~ {} | {}초 범위)",
          since, until, since, until, (until - since) / 1000.0);

      log.info("[Batch-Trigger] [EXECUTE] 배치 작업 실행: batchId={}, username={}, isFirstTime={}",
          batchId, user.getUsername(), isFirstTime);
      JobParameters params = paramsBuilder.toJobParameters();
      jobLauncher.run(lichessJob, params);

      log.info("[Batch-Trigger] [SUCCESS] 배치 작업 실행 완료: batchId={}", batchId);

      // 배치 완료 후 실제 저장된 데이터 조회 및 로깅
      log.info("[Batch-Trigger] [DB-VERIFY] 저장된 스트릭 데이터 검증 중...");
      List<UserDailyStreak> savedStreaks = userDailyStreakRepository.findByUserId(userId);
      if (savedStreaks != null && !savedStreaks.isEmpty()) {
        log.info("[Batch-Trigger] [DB-DATA] 저장된 스트릭 총 {}개", savedStreaks.size());
        // 최근 10개만 표시
        savedStreaks.stream()
            .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
            .limit(10)
            .forEach(streak ->
                log.info("  - date={}, win={}, lose={}, draw={}, lastGameAt={}, lastRating={}",
                    streak.getDate(), streak.getWin(), streak.getLose(), streak.getDraw(),
                    streak.getLastGameAt(), streak.getLastRating())
            );
      }

      log.info("[Batch-Trigger] ========== 배치 작업 트리거 완료 ==========");

    } catch (Exception e) {
      log.error("[Batch-Trigger] [ERROR] 배치 작업 실행 실패 - userId={}, message={}",
          userId, e.getMessage(), e);
      throw new RuntimeException("Batch 실행 실패", e);
    }
  }
}
