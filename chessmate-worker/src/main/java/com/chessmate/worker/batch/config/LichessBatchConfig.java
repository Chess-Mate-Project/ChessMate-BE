package com.chessmate.worker.batch.config;

import com.chessmate.external.dto.game.LichessGamesDto;
import com.chessmate.worker.batch.dto.GameStat;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class LichessBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

  /**
   * 비동기 실행을 위한 JobLauncher 설정
   */
  @Bean(name = "asyncJobLauncher")
  public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
    TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
    jobLauncher.setJobRepository(jobRepository);
    jobLauncher.setTaskExecutor(batchTaskExecutor()); // 비동기 스레드 풀 할당
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  /**
   * 배치 전용 스레드 풀 (동시 가입자 처리 제한)
   */
  @Bean
  public TaskExecutor batchTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);   // 기본적으로 동시에 처리할 유저 수
    executor.setMaxPoolSize(10);  // 최대 동시 처리 유저 수
    executor.setQueueCapacity(100); // 대기열
    executor.setThreadNamePrefix("lichess-batch-");
    executor.initialize();
    return executor;
  }

  @Bean
  public Job lichessGamesJob(Step lichessGamesStep) {
    return new JobBuilder("lichessGamesJob", jobRepository)
        .start(lichessGamesStep)
        .build();
  }

  @Bean
  public Step lichessGamesStep(
      ItemReader<LichessGamesDto> reader,
      ItemProcessor<LichessGamesDto, GameStat> processor,
      ItemWriter<GameStat> writer
  ) {
    return new StepBuilder("lichessGamesStep", jobRepository)
        .<LichessGamesDto, GameStat>chunk(500, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
  }
}