package com.chessmate.api.oauth.common;

import com.chessmate.api.oauth.common.dto.OAuthUrlResponse;

public interface PlatFormOAuthService {
  public OAuthUrlResponse getOAuthUrl();
  public void callback(String code, String state);
}
