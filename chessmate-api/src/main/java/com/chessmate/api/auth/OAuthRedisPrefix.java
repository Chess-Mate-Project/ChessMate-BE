package com.chessmate.api.auth;

public enum OAuthRedisPrefix {
  LICHESS_OAUTH_TOKEN("oauth:lichess:%d:oauth_token"),
  CHESSCOM_OAUTH_TOKEN("oauth:chesscom:%d:oauth_token");

  private final String prefix;

  OAuthRedisPrefix(String prefix) {
    this.prefix = prefix;
  }

  public String bind(Object... args) {
    return String.format(prefix, args);
  }
  }
