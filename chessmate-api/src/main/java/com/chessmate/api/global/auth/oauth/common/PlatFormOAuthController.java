package com.chessmate.api.global.auth.oauth.common;

import com.chessmate.api.global.auth.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.ResponseEntity;

public interface PlatFormOAuthController {
  public ResponseEntity<SuccessResponse<OAuthUrlResponse>> getOAuthUrl();
  public void getCode( String code, String state, HttpServletResponse res ) throws IOException;

}
