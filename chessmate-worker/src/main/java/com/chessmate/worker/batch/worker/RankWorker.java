package com.chessmate.worker.batch.worker;

import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.worker.batch.service.UpdateDataService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RankWorker {

  private final UpdateDataService updateDataService;
  private final UserRepositoryImpl userRepository;
  // 30분 간격으로 변경 (매시 0분, 30분)
  @Scheduled(cron = "0 0 * * * ?")
  public void updateRanking() {
    log.info("랭킹 업데이트 시작");

    List<User> recentUsers = userRepository.findRecentLoginUsersWithin3Days();

    for (User user : recentUsers) {
      log.info("증분 업데이트 대상 사용자: {}", user.getUsername());
      updateDataService.updateUserGameData(user);
    }
  }
}
