package com.chessmate.api.global.auth.repository;

import com.chessmate.api.global.auth.service.AuthCodeInfo;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthRedisRepository {

  private final RedisService redisService;

  public void saveAuthCode(String code, AuthCodeInfo authCodeInfo, int expiration) {
    String key = AuthRedisPrefix.AUTH_CODE.getCompleteKey(code);
    redisService.save(key, authCodeInfo, expiration);
  }

  public AuthCodeInfo getAuthCode(String code) {
    String key = AuthRedisPrefix.AUTH_CODE.getCompleteKey(code);
    return redisService.get(key, AuthCodeInfo.class);
  }
}

