package com.chessmate.redis;

import com.chessmate.infra_redis.redis.RedisStreamService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserEventProducer {

  private static final String STREAM_KEY = "user.events";

  private final RedisStreamService redisStreamService;

  public void publishUserCreated(Long userId, String lichessToken) {
    redisStreamService.publish(
        STREAM_KEY,
        Map.of(
            "type", "USER_CREATED",
            "userId", userId.toString(),
            "lichessToken", lichessToken
        )
    );
  }
}
