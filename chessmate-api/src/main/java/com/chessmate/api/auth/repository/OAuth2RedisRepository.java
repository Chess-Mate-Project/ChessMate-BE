package com.chessmate.api.auth.repository;

import com.chessmate.api.auth.OAuth2Provider;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OAuth2RedisRepository {

  private final RedisService redisService;

  public void saveProviderToken(Long id, OAuth2Provider provider, String providerToken, int expiration) {
    String key = "";
    switch(provider) {
      case LICHESS -> key = Oauth2RedisPrefix.LICHESS_TOKEN.getCompleteKey(id);
      case CHESSCOM -> key = Oauth2RedisPrefix.CHESSCOM_TOKEN.getCompleteKey(id);
    }
    redisService.save(key, providerToken, expiration);
  }
}
