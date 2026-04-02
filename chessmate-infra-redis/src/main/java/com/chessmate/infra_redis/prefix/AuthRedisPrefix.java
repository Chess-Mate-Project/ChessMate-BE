package com.chessmate.infra_redis.prefix;

/**
 * 인증 관련 Redis 키 접두사를 관리하는 Enum입니다.
 */
public enum AuthRedisPrefix {

  // 리프레시 토큰 저장 (예: auth:refresh:123)
  REFRESH_TOKEN("auth:refresh:%d");

  private final String prefix;

  AuthRedisPrefix(String prefix) {
    this.prefix = prefix;
  }

  /**
   * 사용자 ID를 받아 Redis 키를 생성합니다.
   *
   * @param userId 사용자의 고유 식별자 (Long)
   * @return 완성된 Redis 키 (예: auth:refresh:1)
   */
  public String createKey(Long userId) {
    return String.format(this.prefix, userId);
  }
}