package com.chessmate.external.config;

import com.chessmate.external.chesscom.ChesscomConfig;
import com.chessmate.external.lichess.LichessConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebclientConfig {
  private final LichessConfig lichessConfig;
  private final ChesscomConfig chesscomConfig;

  @Bean("lichessWebClient")
  public WebClient lichessWebClient() {
    return WebClient.builder()
        .baseUrl(lichessConfig.getBaseApiUrl())
        .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
        .filter((request, next) -> {
          log.info("➡️ WebClient Request");
          log.info("METHOD: {}", request.method());
          log.info("URL: {}", request.url());
          log.info("HEADERS: {}", request.headers());
          return next.exchange(request);
        })
        .build();
  }

  @Bean("chesscomWebClient")
  public WebClient chesscomWebClient() {
    return WebClient.builder()
        .baseUrl(chesscomConfig.getBaseApiUrl())
        .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
        .filter((request, next) -> {
          log.info("➡️ WebClient Request");
          log.info("METHOD: {}", request.method());
          log.info("URL: {}", request.url());
          log.info("HEADERS: {}", request.headers());
          return next.exchange(request);
        })
        .build();
  }



}
