package com.chessmate.api.auth.repository;

public enum Oauth2RedisPrefix {
  LICHESS_TOKEN("oauth:lichess:%d:token"),
  CHESSCOM_TOKEN("oauth:chesscom:%d:token");

  private final String pattern;

  Oauth2RedisPrefix(String pattern) {
    this.pattern = pattern;
  }

  public String getCompleteKey(Long id) {
    return String.format(this.pattern, id);
  }
}