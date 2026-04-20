package com.chessmate.infra_redis.prefix;

/**
 * 인증 관련 Redis 키 접두사를 관리하는 Enum입니다.
 */
public enum AuthRedisPrefix {

  // 리프레시 토큰 저장 (예: auth:refresh:LICHESS:123)
  // platform 포함: Chess.com id=5 와 Lichess id=5 가 같을 때 충돌 방지
  REFRESH_TOKEN("auth:refresh:%s:%d");

  private final String prefix;

  AuthRedisPrefix(String prefix) {
    this.prefix = prefix;
  }

  /**
   * 플랫폼 + 사용자 ID를 받아 Redis 키를 생성합니다.
   *
   * @param platform 플랫폼 (LICHESS / CHESSCOM)
   * @param userId   사용자의 고유 식별자 (Long)
   * @return 완성된 Redis 키 (예: auth:refresh:LICHESS:5)
   */
  public String createKey(String platform, Long userId) {
    return String.format(this.prefix, platform, userId);
  }
}