package com.chessmate.api.global.auth.oauth.chesscom;

import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.api.global.auth.oauth.common.PlatFormOAuthController;
import com.chessmate.api.global.auth.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.common.response.SuccessResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login/oauth2")
@RequiredArgsConstructor
public class ChesscomOauthController implements PlatFormOAuthController {

  @Value("${client.url}")
  private String clientUrl;

  private final ChesscomOAuthService chesscomOAuthService;

  @GetMapping("/chesscom/url")
  public ResponseEntity<SuccessResponse<OAuthUrlResponse>> getOAuthUrl() {

    OAuthUrlResponse response = chesscomOAuthService.getOAuthUrl();

    return ResponseEntity.ok(new SuccessResponse<>("Chess.com의 OAuthUrl을 제공합니다.", response));
  }

  @GetMapping("/code/chesscom")
  public void getCode(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      HttpServletResponse res
  ) throws IOException {

    TokenResponse response = chesscomOAuthService.callback(code, state);


    Cookie accessCookie = new Cookie("chessladder_chesscom_access_token", response.accessToken());
    accessCookie.setHttpOnly(true);
    accessCookie.setSecure(true);
    accessCookie.setPath("/");
    accessCookie.setMaxAge((int) response.accessTokenExpiresIn());
    accessCookie.setAttribute("SameSite", "Lax");

    Cookie refreshCookie = new Cookie("chessladder_chesscom_refresh_token", response.refreshToken());
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(true);
    refreshCookie.setPath("/api/auth/refresh");
    refreshCookie.setMaxAge((int) response.refreshTokenExpiresIn());
    refreshCookie.setAttribute("SameSite", "Lax");

    res.addCookie(accessCookie);
    res.addCookie(refreshCookie);

    res.sendRedirect(clientUrl);
  }

}
