package com.chessmate.worker.batch.service;

import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.infra_redis.redis.CacheService;
import com.chessmate.infra_redis.redis.LichessApiRedisService;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateDataService {


  private final CacheService cacheService;
  private final LichessApiRedisService lichessApiRedisService;

  public void updateUserGameData(User user, String batchId) {
    String cachedToken = cacheService.getLichessToken(user.getId());
    if (cachedToken == null || cachedToken.isEmpty()) {
      log.info("사용자 : {} id : {} 의 Lichess 토큰이 없습니다. 건너뜁니다.", user.getUsername(), user.getId());
      return;
    }
    LichessApiTask gamestask = LichessApiTask.builder()
        .userId(user.getId())
        .username(user.getUsername())
        .lichessToken(cachedToken)
        .type(TaskType.GAMES)
        .isFullSync(false)
        .batchId(batchId)
        .taskId(java.util.UUID.randomUUID().toString())
        .build();

    LichessApiTask accounttask = LichessApiTask.builder()
        .userId(user.getId())
        .username(user.getUsername())
        .lichessToken(cachedToken)
        .type(TaskType.ACCOUNT)
        .isFullSync(false)
        .batchId(batchId)
        .taskId(java.util.UUID.randomUUID().toString())
        .build();

    LichessApiTask perftask = LichessApiTask.builder()
        .userId(user.getId())
        .username(user.getUsername())
        .lichessToken(cachedToken)
        .type(TaskType.PERF)
        .isFullSync(false)
        .batchId(batchId)
        .taskId(java.util.UUID.randomUUID().toString())
        .build();

    lichessApiRedisService.pushTask(gamestask);
    lichessApiRedisService.pushTask(accounttask);
    lichessApiRedisService.pushTask(perftask);
  }
}
