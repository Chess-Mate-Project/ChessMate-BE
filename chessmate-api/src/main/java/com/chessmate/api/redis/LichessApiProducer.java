package com.chessmate.api.redis;

import com.chessmate.domain.user.User;
import com.chessmate.infra_redis.redis.LichessApiRedisService;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LichessApiProducer {

  private final LichessApiRedisService lichessApiService;

  public void sendSyncTask(User user, String username, String token, TaskType taskType, boolean isFirstTime) {

    // task생성
    LichessApiTask task = LichessApiTask.builder()
        .userId(user.getId())
        .lichessToken(token)
        .username(username)
        .type(taskType)
        .isFullSync(isFirstTime)
        .build();

    // Redis큐로 task 전송
    lichessApiService.pushTask(task);

  }
}
