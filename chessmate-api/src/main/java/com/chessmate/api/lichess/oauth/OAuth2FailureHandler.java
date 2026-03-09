package com.chessmate.api.lichess.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Value("${client.url}")
  private String clientUrl;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException {

    log.error("[OAuth2 로그인 실패] 원인: {}", exception.getMessage());

    String targetUrl = clientUrl + "/login-failure?error=" + exception.getMessage();

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}