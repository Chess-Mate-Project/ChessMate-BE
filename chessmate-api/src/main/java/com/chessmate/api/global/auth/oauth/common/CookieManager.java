package com.chessmate.api.global.auth.oauth.common;

import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.common.dto.OAuthPlatForm;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieManager {
  public void addAuthCookies(HttpServletResponse response, TokenResponse tokenResponse, OAuthPlatForm provider) {
    // Access Token Cookie
    // expiresIn은 밀리초 단위이므로 초 단위로 변환 (1000으로 나눔)
    Cookie accessCookie = createCookie(
        CookieName.ACCESS_TOKEN.of(provider),
        tokenResponse.accessToken(),
        "/",
        (int) (tokenResponse.accessTokenExpiresIn() / 1000)
    );
    // Refresh Token Cookie
    Cookie refreshCookie = createCookie(
        CookieName.REFRESH_TOKEN.of(provider),
        tokenResponse.refreshToken(),
        "/api/auth/refresh",
        (int) (tokenResponse.refreshTokenExpiresIn() / 1000)
    );

    response.addCookie(accessCookie);
    response.addCookie(refreshCookie);
  }

  private Cookie createCookie(String name, String value, String path, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath(path);
    cookie.setMaxAge(maxAge);
    cookie.setAttribute("SameSite", "Lax");
    return cookie;
  }
}
