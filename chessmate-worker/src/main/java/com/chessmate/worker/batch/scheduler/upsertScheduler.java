package com.chessmate.worker.batch.scheduler;

import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.domain.userDailyStreak.UserDailyStreakRepository;
import com.chessmate.infra_persistence.repositoryImpl.UserDailyStreakRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.infra_redis.redis.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class upsertScheduler {

  private final UserRepositoryImpl userRepository;
  private final UserDailyStreakRepository userDailyStreakRepository;
//  @Scheduled(fixedRate = 2)
//  public void updateGames(
//
//  ) {
//    List<User> users =
//      userDailyStreakRepository.
//  }

}
