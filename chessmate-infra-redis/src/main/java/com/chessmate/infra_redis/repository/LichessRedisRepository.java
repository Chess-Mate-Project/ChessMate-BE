package com.chessmate.infra_redis.repository;

import com.chessmate.external.dto.lichess.LichessTokenResponse;
import com.chessmate.infra_redis.prefix.LichessPrefix;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * Lichess OAuth 토큰을 Redis에 저장하는 Repository
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LichessRedisRepository {

  private final RedisService redisService;

  public void saveAccessToken(String lichessId, String accessToken, int expiresInSeconds) {
    String key = LichessPrefix.ACCESS_TOKEN.createKey(lichessId);
    redisService.save(key, accessToken, expiresInSeconds);
  }

  public String getAccessToken(String lichessId) {
    String key = LichessPrefix.ACCESS_TOKEN.createKey(lichessId);
    return redisService.get(key, String.class);
  }

  public void deleteAccessToken(String lichessId) {
    String key = LichessPrefix.ACCESS_TOKEN.createKey(lichessId);
    redisService.delete(key);
  }


}

