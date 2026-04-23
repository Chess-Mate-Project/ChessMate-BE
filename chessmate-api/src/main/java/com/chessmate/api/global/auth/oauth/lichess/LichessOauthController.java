package com.chessmate.api.global.auth.oauth.lichess;

import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.api.global.auth.oauth.common.CookieManager;
import com.chessmate.api.global.auth.oauth.common.PlatFormOAuthController;
import com.chessmate.api.global.auth.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.exception.AuthException;
import com.chessmate.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
@Slf4j
public class LichessOauthController implements PlatFormOAuthController {

  @Value("${client.url}")
  private String clientUrl;

  private final LichessOAuthService lichessOAuthService;
  private final CookieManager cookieManager;

  @GetMapping("/lichess/url")
  public ResponseEntity<SuccessResponse<OAuthUrlResponse>> getOAuthUrl() {

    OAuthUrlResponse response = lichessOAuthService.getOAuthUrl();

    return ResponseEntity.ok(new SuccessResponse<>("Lichess의 OAuthUrl을 제공합니다.", response));
  }

  @GetMapping("/lichess/callback")
  public void getCode(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      HttpServletResponse res
  ) throws IOException {
    try {
      TokenResponse response = lichessOAuthService.callback(code, state);
      cookieManager.addAuthCookies(res, response, OAuthPlatForm.LICHESS);
      res.sendRedirect(clientUrl);
    } catch (AuthException e) {
      log.warn("[LichessCallback] 인증 실패, 로그인 페이지로 redirect - {}", e.getMessage());
      res.sendRedirect(clientUrl + "/?error=auth_failed");
    } catch (Exception e) {
      log.error("[LichessCallback] 예상치 못한 에러, 로그인 페이지로 redirect", e);
      res.sendRedirect(clientUrl + "/?error=server_error");
    }
  }
}

