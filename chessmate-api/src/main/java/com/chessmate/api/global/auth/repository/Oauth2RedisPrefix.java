package com.chessmate.api.global.auth.repository;

public enum Oauth2RedisPrefix {
  LICHESS_TOKEN("oauth:lichess:%d:token"),
  CHESSCOM_TOKEN("oauth:chesscom:%d:token"),
  PCKE_VERIFIER("oauth:code_verifier:%s");

  private final String pattern;

  Oauth2RedisPrefix(String pattern) {
    this.pattern = pattern;
  }

  /**
   * 주어진 값을 패턴에 따라 Redis 키로 변환합니다.
   *
   * @param value 키에 포함될 값 (Long, String, 또는 다른 타입)
   * @return 완성된 Redis 키
   */
  public String getCompleteKey(Object value) {
    return String.format(this.pattern, value);
  }
}