package com.chessmate.api.global.auth.repository;

public enum AuthRedisPrefix {
  AUTH_CODE("auth:code:%s");

  private final String prefix;

  AuthRedisPrefix(String prefix) {
    this.prefix = prefix;
  }

  /**
   * 주어진 값을 패턴에 따라 Redis 키로 변환합니다.
   *
   * @param value 키에 포함될 값 (String, Long, 또는 다른 타입)
   * @return 완성된 Redis 키
   */
  public String getCompleteKey(Object value) {
    return String.format(this.prefix, value);
  }
}
