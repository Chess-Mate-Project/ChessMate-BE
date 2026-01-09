package com.chessmate.worker.batch.service;


import com.chessmate.common.service.UserBatchService;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserDailyStreakRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBatchServiceImpl implements UserBatchService {

  private final JobLauncher jobLauncher;
  private final Job lichessJob;
  private final UserRepositoryImpl userRepository;
  private final UserDailyStreakRepositoryImpl userDailyStreakRepository;

  @Override
  public void triggerUserUpdate(Long userId, String lichessToken) {

    User user = userRepository.findById(userId).orElseThrow();

    try {
      JobParameters params = new JobParametersBuilder()
          .addString("lichess_id", user.getLichessId())
          .addString("token", lichessToken)
          .toJobParameters();

      jobLauncher.run(lichessJob, params);

    } catch (Exception e) {
      throw new RuntimeException("Batch 실행 실패", e);
    }
  }
}
