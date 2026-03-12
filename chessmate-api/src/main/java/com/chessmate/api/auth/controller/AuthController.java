package com.chessmate.api.auth.controller;

import static com.chessmate.api.auth.jwt.JwtRule.REFRESH_PREFIX;

import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.auth.dto.TokenResponse;
import com.chessmate.api.auth.service.AuthService;
import com.chessmate.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  @Value("${cookie.domain}")
  private String cookieDomain;

  @GetMapping("token")
  public ResponseEntity<SuccessResponse<TokenResponse>> getToken(
      @Param("code") String code,
      HttpServletResponse res
  ) {

    TokenResponse response = authService.getToken(code);

    ResponseCookie cookie = ResponseCookie.from(REFRESH_PREFIX.getValue(), response.refreshToken())
        .path("/")
        .domain(cookieDomain)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .maxAge(response.refreshTokenExpiresIn() / 1000)
        .build();

    res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    return ResponseEntity.ok(
        new SuccessResponse<>("토큰 발급 성공", response)
    );
  }

  /*@PostMapping("logout")
  public ResponseEntity<SuccessResponse<Void>> logout(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      HttpServletResponse res
  ) {
      authService.logout(userPrincipal.getUser(), res);

      return ResponseEntity.ok(
          new SuccessResponse<>("로그아웃 성공", null)
      );
  }

  @GetMapping ("refresh")
  public ResponseEntity<SuccessResponse<Void>> refreshToken(
      HttpServletResponse res,
      HttpServletRequest req
  ) {
      authService.refresh(req, res);

      return ResponseEntity.ok(
          new SuccessResponse<>("토큰 재발급 성공", null)
      );

  }

  @GetMapping("/me")
  public ResponseEntity<SuccessResponse<User>> me(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
      return ResponseEntity.ok(
          new SuccessResponse<>("인증되어 있습니다.", userPrincipal.getUser())
      );
  }*/
}
