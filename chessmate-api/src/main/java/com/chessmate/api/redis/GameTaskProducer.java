package com.chessmate.api.redis;

import com.chessmate.common.dto.GameSyncTaskDto;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_redis.redis.QueueKey;
import com.chessmate.infra_redis.redis.RedisService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 게임 동기화 작업을 Redis Queue에 넣어서
 * Worker 서버로 지시하는 Producer 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameTaskProducer {

  private final RedisService redisService;

  /**
   * 새로운 사용자 회원가입 시 게임 데이터 동기화 작업을 Queue에 추가합니다.
   *
   * @param platform         플랫폼명 ("LICHESS" 또는 "CHESSCOM")
   * @param platformUsername   플랫폼의 사용자 ID (username)
   * @param serviceUserId    서비스의 사용자 ID (DB에 저장된 ID)
   */
  public void enqueueNewUserGameSync(OAuthPlatForm platform, String platformUsername, Long serviceUserId) {
    String taskId = generateTaskId(platform, platformUsername, serviceUserId);

    GameSyncTaskDto task = GameSyncTaskDto.builder()
        .platform(platform)
        .platformUsername(platformUsername)
        .serviceUserId(serviceUserId)
        .createdAt(System.currentTimeMillis())
        .priority(1) // 신규 사용자는 우선순위 높음
        .retryCount(0)
        .taskId(taskId)
        .build();

    redisService.enqueue(QueueKey.GAME_SYNC_TASKS.getKey(), task);

    log.info(
        "[Game Sync Task] 새 사용자 게임 동기화 작업 추가 - "
            + "taskId={}, platform={}, platformUsername={}, serviceUserId={}",
        taskId,
        platform,
        platformUsername,
        serviceUserId
    );
  }

  /**
   * 작업의 고유한 ID를 생성합니다.
   * 중복 방지 및 추적 용도로 사용됩니다.
   */
  private String generateTaskId(OAuthPlatForm platform, String platformUserId, Long serviceUserId) {
    return String.format(
        "%s-%s-%d-%s",
        platform,
        platformUserId,
        serviceUserId,
        UUID.randomUUID().toString().substring(0, 8)
    );
  }
}

