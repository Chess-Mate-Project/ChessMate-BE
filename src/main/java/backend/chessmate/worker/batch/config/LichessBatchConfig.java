package backend.chessmate.worker.batch.config;

import backend.chessmate.global.external.dto.game.LichessGamesDto;
import backend.chessmate.worker.batch.dto.GameStat;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class LichessBatchConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;

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
