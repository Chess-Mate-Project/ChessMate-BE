package com.chessmate.api.oauth.common;

import com.chessmate.api.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.common.response.SuccessResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface PlatFormOAuthController {
  public ResponseEntity<SuccessResponse<OAuthUrlResponse>> getOAuthUrl();
  public void getCode( String code, String state );
}
