package backend.chessmate.global.external.config;

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

  @Bean
  public WebClient lichessWebClient() {
    return WebClient.builder()
        .baseUrl(lichessConfig.getBaseApiUrl())
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
