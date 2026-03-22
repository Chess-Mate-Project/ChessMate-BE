package com.chessmate.api.oauth.chesscom;

import com.chessmate.api.oauth.common.PlatFormOAuthController;
import com.chessmate.api.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login/oauth2")
@RequiredArgsConstructor
public class ChesscomOauthController implements PlatFormOAuthController {

  private final ChesscomOAuthService chesscomOAuthService;

  @GetMapping("/chesscom/url")
  public ResponseEntity<SuccessResponse<OAuthUrlResponse>> getOAuthUrl() {

    OAuthUrlResponse response = chesscomOAuthService.getOAuthUrl();

    return ResponseEntity.ok(new SuccessResponse<>("Chess.com의 OAuthUrl을 제공합니다.", response));
  }

  @GetMapping("/code/chesscom")
  public void getCode(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state
  ) {

    chesscomOAuthService.callback(code, state);
  }

}
