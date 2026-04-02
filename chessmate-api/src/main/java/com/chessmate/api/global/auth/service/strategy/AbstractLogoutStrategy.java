package com.chessmate.api.global.auth.service.strategy;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractLogoutStrategy implements LogoutStrategy {

  protected void deleteCookie(HttpServletResponse response, String cookieName, String path) {
    Cookie cookie = new Cookie(cookieName, null);
    cookie.setPath(path); // 전달받은 경로 설정
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    cookie.setSecure(true); // 생성 시 설정과 동일하게 맞춤
    response.addCookie(cookie);
  }
}