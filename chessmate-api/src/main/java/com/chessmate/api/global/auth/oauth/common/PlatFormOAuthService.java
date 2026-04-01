package com.chessmate.api.global.auth.oauth.common;

import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.api.global.auth.oauth.common.dto.OAuthUrlResponse;

public interface PlatFormOAuthService {
  public OAuthUrlResponse getOAuthUrl();
  public TokenResponse callback(String code, String state);
}
