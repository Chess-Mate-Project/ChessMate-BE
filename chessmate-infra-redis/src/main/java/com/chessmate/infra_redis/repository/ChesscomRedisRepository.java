package com.chessmate.infra_redis.repository;

import com.chessmate.external.dto.chesscom.ChesscomTokenResponse;
import com.chessmate.infra_redis.prefix.ChesscomPrefix;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * Chess.com OAuth 토큰을 Redis에 저장하는 Repository
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChesscomRedisRepository {

  private final RedisService redisService;

  public void saveAccessToken(Long chesscomId, String accessToken, int expiration) {
    String key = ChesscomPrefix.ACCESS_TOKEN.createKey(chesscomId);
    redisService.save(key, accessToken, expiration);
  }

  public void saveRefreshToken(Long chesscomId, String refreshToken, int expiration) {
    String key = ChesscomPrefix.REFRESH_TOKEN.createKey(chesscomId);
    redisService.save(key, refreshToken, expiration);
  }

  public String getAccessToken(Long chesscomId) {
    String key = ChesscomPrefix.ACCESS_TOKEN.createKey(chesscomId);
    return redisService.get(key, String.class);
  }

  public String getRefreshToken(Long chesscomId) {
    String key = ChesscomPrefix.REFRESH_TOKEN.createKey(chesscomId);
    return redisService.get(key, String.class);
  }

  public void deleteAccessToken(Long chesscomId) {
    String key = ChesscomPrefix.ACCESS_TOKEN.createKey(chesscomId);
    redisService.delete(key);
  }

  public void deleteRefreshToken(Long chesscomId) {
    String key = ChesscomPrefix.REFRESH_TOKEN.createKey(chesscomId);
    redisService.delete(key);
  }

}

