package backend.chessmate.worker.batch.service;


import backend.chessmate.api.user.entity.User;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBatchService {

  private final JobLauncher jobLauncher;
  private final Job lichessJob;

  public void runUserBatch(User user, String lichessToken) {
    try {
      JobParameters params = new JobParametersBuilder()
          .addString("username", user.getUsername())
          .addString("token", lichessToken)
          .addLong("time", System.currentTimeMillis()) // 항상 새로운 Job 실행을 위해
          .toJobParameters();

      jobLauncher.run(lichessJob, params);

    } catch (Exception e) {
      throw new RuntimeException("Batch 실행 실패", e);
    }
  }
}
