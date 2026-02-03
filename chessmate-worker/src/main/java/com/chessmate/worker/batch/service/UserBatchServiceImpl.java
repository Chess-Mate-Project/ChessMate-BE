package com.chessmate.worker.batch.service;


import com.chessmate.common.service.UserBatchService;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserDailyStreakRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

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

    User user = userRepository.findById(userId).orElseThrow();
    Long since = null;
    if(isFirstTime) {
      since =  userDailyStreakRepository.findLastGameAtByUserId(userId);
    }

    try {
      JobParameters params = new JobParametersBuilder()
          .addString("username", user.getUsername())
          .addString("token", lichessToken)
          .addLong("since", since)
          .toJobParameters();

      jobLauncher.run(lichessJob, params);

    } catch (Exception e) {
      throw new RuntimeException("Batch 실행 실패", e);
    }
  }
}
