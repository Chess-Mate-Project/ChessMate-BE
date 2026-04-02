package com.chessmate.api.global.auth.oauth.common;

import com.chessmate.common.dto.OAuthPlatForm;

public enum CookieName {
  REFRESH_TOKEN("CHESSLADDER_%s_REFRESH"),
  ACCESS_TOKEN("CHESSLADDER_%s_ACCESS");

  private final String pattern;

  CookieName(String pattern) {
    this.pattern = pattern;
  }

  public String of(OAuthPlatForm provider) {
    return String.format(this.pattern, provider.name().toUpperCase());
  }
}