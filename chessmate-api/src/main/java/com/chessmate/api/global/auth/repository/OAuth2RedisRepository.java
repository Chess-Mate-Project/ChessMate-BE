package com.chessmate.api.global.auth.repository;

import com.chessmate.api.global.auth.OAuth2Provider;
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

  public void saveCodeVerifier(String state, String codeVerifier, int expiration) {
    String key = Oauth2RedisPrefix.PCKE_VERIFIER.getCompleteKey(state);
    redisService.save(key, codeVerifier, expiration);
  }

  /**
   * 저장된 PKCE Code Verifier를 조회합니다.
   *
   * @param state OAuth state 파라미터
   * @return 저장된 code verifier (없으면 null)
   */
  public String getCodeVerifier(String state) {
    String key = Oauth2RedisPrefix.PCKE_VERIFIER.getCompleteKey(state);
    return redisService.get(key, String.class);
  }

  /**
   * PKCE Code Verifier를 삭제합니다.
   * 토큰 교환 후 사용된 code verifier를 제거합니다.
   *
   * @param state OAuth state 파라미터
   */
  public void deleteCodeVerifier(String state) {
    String key = Oauth2RedisPrefix.PCKE_VERIFIER.getCompleteKey(state);
    redisService.delete(key);
  }
}
