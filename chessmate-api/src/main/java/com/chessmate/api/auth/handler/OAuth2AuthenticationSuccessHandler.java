package com.chessmate.api.auth.handler;

import com.chessmate.api.auth.OAuth2Provider;
import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.auth.jwt.JwtService;
import com.chessmate.api.auth.repository.AuthRedisPrefix;
import com.chessmate.api.auth.repository.AuthRedisRepository;
import com.chessmate.api.auth.service.AuthCodeInfo;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.infra_persistence.lichess.user.repositoryImpl.LichessUserRepositoryImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties.Http;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtService jwtService;
  private final AuthRedisRepository authRedisRepository;
  private final LichessUserRepositoryImpl lichessUserRepository;
//  private final ChesscomUserRepositoryImpl chesscomUserRepository;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

    if (principal.getProvider().equals(OAuth2Provider.LICHESS)) {
      LichessUser luser = lichessUserRepository.findById(principal.getId())
          .orElseThrow(() -> new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT));

      String tempCode = UUID.randomUUID().toString();
      AuthCodeInfo authCodeInfo = new AuthCodeInfo(
          luser.getId(),
          principal.getProvider()
      );

      authRedisRepository.saveAuthCode(tempCode, authCodeInfo , 10);

      String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/login/callback")
          .queryParam("code", tempCode)
          .build().toUriString();

      getRedirectStrategy().sendRedirect(request, response, targetUrl);


    } else if (principal.getProvider().equals(OAuth2Provider.CHESSCOM)) {

    }


  }


}
