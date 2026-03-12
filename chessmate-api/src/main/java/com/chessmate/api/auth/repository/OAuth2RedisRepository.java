package com.chessmate.api.auth.repository;

import com.chessmate.api.auth.OAuth2Provider;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OAuth2RedisRepository {

  private RedisService redisService;

  public void saveProviderToken(Long id, OAuth2Provider provider, String providerToken, Long expiration) {
    String key = Oauth2RedisPrefix.CHESSCOM_TOKEN.getCompleteKey(id);

  }
}
