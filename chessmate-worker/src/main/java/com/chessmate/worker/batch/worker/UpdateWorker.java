package com.chessmate.worker.batch.worker;

import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.worker.batch.service.BatchBarrierService;
import com.chessmate.worker.batch.service.UpdateDataService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateWorker {

  private final UpdateDataService updateDataService;
  private final UserRepositoryImpl userRepository;
  private final BatchBarrierService batchBarrierService;

  @Scheduled(cron = "0 0/30 * * * ?")
  public void updateUserData() {
    log.info("게임 스트릭 조회로 증분 업데이트 시작");

    List<User> recentUsers = userRepository.findRecentLoginUsersWithin3Days();

    String batchId = UUID.randomUUID().toString();
    long expected = (long) recentUsers.size() * 2L; // ACCOUNT + PERF
    batchBarrierService.initBatch(batchId, expected);

    for (User user : recentUsers) {
      log.info("증분 업데이트 대상 사용자: {}", user.getUsername());
      updateDataService.updateUserGameData(user, batchId);
    }
  }
}