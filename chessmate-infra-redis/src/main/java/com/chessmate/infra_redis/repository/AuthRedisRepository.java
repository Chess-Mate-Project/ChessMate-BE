package com.chessmate.infra_redis.repository;


import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_redis.prefix.AuthRedisPrefix;
import com.chessmate.infra_redis.prefix.ChesscomPrefix;
import com.chessmate.infra_redis.prefix.LichessPrefix;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthRedisRepository {

  private final RedisService redisService;

  public void saveLichessAccessToken(Long userId, String accessToken, int expiresInSeconds) {
    String key = LichessPrefix.ACCESS_TOKEN.createKey(userId);
    redisService.save(key, accessToken, expiresInSeconds);
  }

  public String getLichessAccessToken(Long userId) {
    String key = LichessPrefix.ACCESS_TOKEN.createKey(userId);
    return redisService.get(key, String.class);
  }

  public void deleteLichessAccessToken(Long userId) {
    String key = LichessPrefix.ACCESS_TOKEN.createKey(userId);
    redisService.delete(key);
  }

  public void saveChesscomAccessToken(Long userId, String accessToken, int expiration) {
    String key = ChesscomPrefix.ACCESS_TOKEN.createKey(userId);
    redisService.save(key, accessToken, expiration);
  }

  public void saveChesscomRefreshToken(Long userId, String refreshToken, int expiration) {
    String key = ChesscomPrefix.REFRESH_TOKEN.createKey(userId);
    redisService.save(key, refreshToken, expiration);
  }

  public String getChesscomAccessToken(Long userId) {
    String key = ChesscomPrefix.ACCESS_TOKEN.createKey(userId);
    return redisService.get(key, String.class);
  }

  public String getChesscomRefreshToken(Long userId) {
    String key = ChesscomPrefix.REFRESH_TOKEN.createKey(userId);
    return redisService.get(key, String.class);
  }

  public void deleteChesscomAccessToken(Long userId) {
    String key = ChesscomPrefix.ACCESS_TOKEN.createKey(userId);
    redisService.delete(key);
  }

  public void deleteChesscomRefreshToken(Long userId) {
    String key = ChesscomPrefix.REFRESH_TOKEN.createKey(userId);
    redisService.delete(key);
  }



  public void saveRefreshToken(Long userId, OAuthPlatForm platform, String refreshToken, int expiration) {
    String key = AuthRedisPrefix.REFRESH_TOKEN.createKey(platform.name(), userId);
    redisService.save(key, refreshToken, expiration);
  }

  public String getRefreshToken(Long userId, OAuthPlatForm platform) {
    String key = AuthRedisPrefix.REFRESH_TOKEN.createKey(platform.name(), userId);
    return redisService.get(key, String.class);
  }

  public void deleteRefreshToken(Long userId, OAuthPlatForm platform) {
    String key = AuthRedisPrefix.REFRESH_TOKEN.createKey(platform.name(), userId);
    redisService.delete(key);
  }

}

