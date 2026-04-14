package com.chessmate.api.global.auth.controller;


import com.chessmate.api.global.auth.dto.MeResponse;
import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.global.auth.service.AuthService;
import com.chessmate.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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


  @PostMapping("logout")
  public ResponseEntity<SuccessResponse<Void>> logout(
      @AuthenticationPrincipal UserPrincipal userPrincipal,
      HttpServletResponse res
  ) {
      authService.logout(userPrincipal, res);

      return ResponseEntity.ok(
          new SuccessResponse<>("로그아웃 성공", null)
      );
  }

  @GetMapping("refresh")
  public ResponseEntity<SuccessResponse<Void>> refreshToken(
      HttpServletResponse res,
      HttpServletRequest req
  ) {
      authService.refresh(req, res);

      return ResponseEntity.ok(
          new SuccessResponse<>("토큰 재발급 성공", null)
      );

  }
  /**
   * GET /api/auth/me
   *
   * 현재 인증 상태 확인 및 사용자 정보 반환.
   * 200 → 인증됨, 401 → 미인증 (쿠키 없음 또는 만료)
   */
  @GetMapping("/me")
  public ResponseEntity<SuccessResponse<MeResponse>> getMe(
      @AuthenticationPrincipal UserPrincipal userPrincipal
  ) {
    if (userPrincipal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    MeResponse response = authService.getMe(userPrincipal);
    return ResponseEntity.ok(new SuccessResponse<>("사용자 정보 조회 성공", response));
  }
}
