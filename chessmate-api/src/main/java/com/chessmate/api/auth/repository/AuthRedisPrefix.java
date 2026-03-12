package com.chessmate.api.auth.repository;

public enum AuthRedisPrefix {
  AUTH_CODE("auth:code:%s");

  private final String prefix;

  AuthRedisPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String getCompleteKey(String code) {
    return String.format(this.prefix, code);
  }
}
