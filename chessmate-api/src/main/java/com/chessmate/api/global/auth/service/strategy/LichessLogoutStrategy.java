package com.chessmate.api.global.auth.service.strategy;

import com.chessmate.api.global.auth.oauth.common.CookieName;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_redis.repository.AuthRedisRepository;
import com.chessmate.infra_redis.repository.OAuth2RedisRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LichessLogoutStrategy extends AbstractLogoutStrategy {

  @Override
  public void logout(Long userId, HttpServletResponse response) {
    deleteCookie(response, CookieName.ACCESS_TOKEN.of(OAuthPlatForm.LICHESS), "/");
    deleteCookie(response, CookieName.REFRESH_TOKEN.of(OAuthPlatForm.LICHESS), "/api/auth/refresh");
  }

  @Override
  public OAuthPlatForm getProvider() {
    return OAuthPlatForm.LICHESS;
  }
}
