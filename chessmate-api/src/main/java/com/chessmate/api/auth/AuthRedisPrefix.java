package com.chessmate.api.auth;

public enum AuthRedisPrefix {
  REFRESH_TOKEN("auth:%d:refresh_token");

  private final String prefix;

  AuthRedisPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String bind(Object... args) {
    return String.format(prefix, args);
  }
}
