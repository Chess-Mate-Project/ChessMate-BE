package com.chessmate.api.oauth.controller;


import com.chessmate.api.oauth.dto.OauthUrlResponse;
import com.chessmate.api.oauth.service.OauthService;
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
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
@Slf4j
public class OauthController {

    @Value("${client.url}")
    private String clientUrl;

    private final OauthService oauthService;

    @GetMapping("/oauth-url")
    public ResponseEntity<SuccessResponse<OauthUrlResponse>> getOauthUrl(
    ) {

       OauthUrlResponse response = oauthService.getOauthUrl();

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

    response.sendRedirect(clientUrl);
  }

}
