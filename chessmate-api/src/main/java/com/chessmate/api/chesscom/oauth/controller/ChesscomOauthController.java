package com.chessmate.api.chesscom.oauth.controller;


import com.chessmate.api.chesscom.oauth.dto.OauthUrlResponse;
import com.chessmate.api.chesscom.oauth.service.ChesscomOauthService;
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
@Slf4j
public class ChesscomOauthController {

    @Value("${client.url}")
    private String clientUrl;

    private final ChesscomOauthService oauthService;

    @GetMapping("/api/oauth/chesscom")
    public ResponseEntity<SuccessResponse<OauthUrlResponse>> getOauthUrl(
    ) {

       OauthUrlResponse response = oauthService.getOauthUrl();

        return ResponseEntity.ok(
            new SuccessResponse<>("OauthUrl 제공", response)
        );
    }

  @GetMapping("/login/oauth/code/chesscom")
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
