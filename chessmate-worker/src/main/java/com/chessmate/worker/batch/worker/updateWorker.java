package com.chessmate.worker.batch.worker;

import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.infra_redis.redis.CacheService;
import com.chessmate.infra_redis.redis.LichessApiRedisService;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
import com.chessmate.worker.batch.service.UpdateDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class updateWorker {
  private final UpdateDataService updateDataService;
  private final UserRepositoryImpl userRepository;
  // 게임 정보 업데이트 작업
  @Scheduled(cron = "0 0 0 * * ?")
  public void updateUserData() {
    log.info("게임 스트릭 조회로 증분 업데이트 시작");

    List<User> recentUsers = userRepository.findRecentLoginUsersWithin3Days();

    for (User user : recentUsers) {
      log.info("증분 업데이트 대상 사용자: {}", user.getUsername());
      updateDataService.updateUserGameData(user);
    }
  }
}
