package com.chessmate.external.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

  @Bean
  public LichessApi lichessApi() {
      RestClient restClient = RestClient.builder()
          .baseUrl("https://lichess.org/api")
          .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
          .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(adapter)
        .build();

    return factory.createClient(LichessApi.class);
  }
}
