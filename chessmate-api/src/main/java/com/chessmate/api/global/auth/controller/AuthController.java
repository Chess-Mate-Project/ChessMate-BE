package com.chessmate.api.global.auth.controller;


import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.global.auth.service.AuthService;
import com.chessmate.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
//
//  @GetMapping("/me")
//  public ResponseEntity<SuccessResponse<User>> me(
//      @AuthenticationPrincipal UserPrincipal userPrincipal
//  ) {
//      return ResponseEntity.ok(
//          new SuccessResponse<>("인증되어 있습니다.", userPrincipal.getUser())
//      );
//  }
}
