package com.chessmate.api.global.auth.service.strategy;

import com.chessmate.api.global.auth.oauth.common.CookieName;
import com.chessmate.common.dto.OAuthPlatForm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChesscomLogoutStrategy extends AbstractLogoutStrategy {

  @Override
  public void logout(Long userId, HttpServletResponse response) {
    deleteCookie(response, CookieName.ACCESS_TOKEN.of(OAuthPlatForm.CHESSCOM), "/");
    deleteCookie(response, CookieName.REFRESH_TOKEN.of(OAuthPlatForm.CHESSCOM),
        "/api/auth/refresh");
  }

  @Override
  public OAuthPlatForm getProvider() {
    return OAuthPlatForm.CHESSCOM;
  }
}
