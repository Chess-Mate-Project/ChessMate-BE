package com.chessmate.api.auth.controller;

import com.chessmate.api.auth.service.AuthService;
import com.chessmate.api.lichess.oauth.OAuth2PrincipalDetails;
import com.chessmate.common.response.SuccessResponse;
import com.chessmate.infra_core.entity.Profile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

  @PostMapping("logout")
  public ResponseEntity<SuccessResponse<Void>> logout(
      @AuthenticationPrincipal OAuth2PrincipalDetails principalDetails,
      HttpServletResponse res
  ) {
      authService.logout(principalDetails.getId(), res);

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
  public ResponseEntity<SuccessResponse<Profile>> me(
      @AuthenticationPrincipal OAuth2PrincipalDetails principalDetails
  ) {
      return ResponseEntity.ok(
          new SuccessResponse<>("인증되어 있습니다.", principalDetails.getId())
      );
  }
}
