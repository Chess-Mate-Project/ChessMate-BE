package com.chessmate.api.lichess.oauth;

import com.chessmate.api.auth.jwt.JwtService;
import com.chessmate.infra_core.entity.Profile;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * OAuth2 인증 성공 핸들러
 * 역할:
 * 1. 서버 accessToken/refreshToken 발급
 * 2. 쿠키에 토큰 저장
 * 3. 클라이언트로 리다이렉트
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtService jwtService;

  @Value("${client.url}")
  private String clientUrl;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    // 1. OAuth2 인증된 사용자 정보 추출
    OAuth2PrincipalDetails oAuth2PrincipalDetails =
        (OAuth2PrincipalDetails) authentication.getPrincipal();
    Profile profile = oAuth2PrincipalDetails.getProfile();

    jwtService.generateAccessToken(response, profile);
    jwtService.generateRefreshToken(response, profile);

    getRedirectStrategy().sendRedirect(request, response, clientUrl);

  }
}
