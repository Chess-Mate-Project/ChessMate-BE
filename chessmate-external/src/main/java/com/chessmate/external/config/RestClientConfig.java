package com.chessmate.external.config;

import com.chessmate.external.oauth.chesscom.ChesscomOauthApi;
import com.chessmate.external.oauth.lichess.LichessOauthApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfig {
  private final LichessProperties lichessProperties;
  private final ChesscomProperties chesscomProperties;

  @Bean("chesscomOauthApi")
  public ChesscomOauthApi chesscomOauthApi() {
    RestClient restClient = RestClient.builder()
        .baseUrl(chesscomProperties.getOauthUrl())
        .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
        .requestInterceptor((request, body, execution) -> {
          log.info("[ChessCom OAuth API] 요청 경로: {} {}", request.getMethod(), request.getURI());
          if (body != null && body.length > 0) {
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            log.info("[ChessCom OAuth API] 요청 Body: {}", bodyStr);
          }
          ClientHttpResponse response = execution.execute(request, body);
          log.info("[ChessCom OAuth API] 응답 상태: {}", response.getStatusCode());
          return response;
        })
        .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(adapter)
        .build();

    return factory.createClient(ChesscomOauthApi.class);
  }

  /**
   * Lichess OAuth API 클라이언트 빈 생성
   * Lichess의 토큰 엔드포인트와 통신하기 위한 REST Client를 구성합니다.
   *
   * @return Lichess OAuth API 프록시
   */
  @Bean("lichessOauthApi")
  public LichessOauthApi lichessOauthApi() {
    RestClient restClient = RestClient.builder()
        .baseUrl(lichessProperties.getOauthUrl())
        .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
        .requestInterceptor((request, body, execution) -> {
          log.info("[Lichess OAuth API] 요청 경로: {} {}", request.getMethod(), request.getURI());
          if (body != null && body.length > 0) {
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            log.info("[Lichess OAuth API] 요청 Body: {}", bodyStr);
          }
          ClientHttpResponse response = execution.execute(request, body);
          log.info("[Lichess OAuth API] 응답 상태: {}", response.getStatusCode());
          return response;
        })
        .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(adapter)
        .build();

    return factory.createClient(LichessOauthApi.class);
  }
}
