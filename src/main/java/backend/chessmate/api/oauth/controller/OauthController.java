package backend.chessmate.api.oauth.controller;

import backend.chessmate.api.auth.UserPrincipal;
import backend.chessmate.api.oauth.dto.response.OauthUrlResponse;
import backend.chessmate.api.oauth.service.OauthService;
import backend.chessmate.global.common.response.SuccessResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
@Slf4j
public class OauthController {

    private final OauthService oauthService;

    @GetMapping("/oauth-url")
    public ResponseEntity<SuccessResponse<OauthUrlResponse>> getOauthUrl(
    ) {

       var response = oauthService.getOauthUrl();

        return ResponseEntity.ok(
            new SuccessResponse<>("OauthUrl 제공", response)
        );
    }

  @GetMapping("/callback")
  public void oauthCallback(
      @RequestParam String code,
      @RequestParam String state,
      HttpServletResponse response
  ) throws IOException {
      log.info("callback");

    oauthService.callback(code, state, response);

    response.sendRedirect("http://localhost:5173/oauth/success");
  }

}
