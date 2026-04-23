package com.chessmate.external.config;

import com.chessmate.external.api.chesscom.ChesscomApi;
import com.chessmate.external.oauth.chesscom.ChesscomOauthApi;
import com.chessmate.external.oauth.lichess.LichessOauthApi;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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
   * Lichess OAuth API 클라이언트 빈 생성 Lichess의 토큰 엔드포인트와 통신하기 위한 REST Client를 구성합니다.
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


  /**
   * Chess.com Public API 클라이언트 빈 생성 (레거시 - 제거 예정) Chess.com의 공개 API 엔드포인트와 통신하기 위한 REST Client를
   * 구성합니다.
   *
   * @return Chess.com API 프록시
   */
  @Bean("chesscomApiProxy")
  public ChesscomApi chesscomApiProxy() {
    RestClient restClient = RestClient.builder()
        .baseUrl(chesscomProperties.getBaseApiUrl())
        .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
        .requestInterceptor((request, body, execution) -> {
          log.info("[ChessCom API] 요청 경로: {} {}", request.getMethod(), request.getURI());
          if (body != null && body.length > 0) {
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            log.info("[ChessCom API] 요청 Body: {}", bodyStr);
          }
          ClientHttpResponse response = execution.execute(request, body);
          log.info("[ChessCom API] 응답 상태: {}", response.getStatusCode());

          if (request.getURI().getPath().endsWith("/archives")) {
            byte[] responseBody = response.getBody().readAllBytes();
            log.info("[ChessCom API] archives 응답 Body: {}", new String(responseBody, StandardCharsets.UTF_8));
            return new ClientHttpResponse() {
              @Override public HttpStatusCode getStatusCode() throws IOException { return response.getStatusCode(); }
              @Override public HttpHeaders getHeaders() { return response.getHeaders(); }
              @Override public InputStream getBody() { return new ByteArrayInputStream(responseBody); }
              @Override public void close() { response.close(); }
            };
          }
          return response;
        })
        .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(adapter)
        .build();

    return factory.createClient(ChesscomApi.class);
  }

  /**
   * Lichess Public API 클라이언트 빈 생성
   * Lichess의 공개 API 엔드포인트와 통신하기 위한 REST Client를 구성합니다.
   * Bearer token을 사용한 인증된 요청을 지원합니다.
   *
   * @return Lichess API 프록시
   */
  @Bean("lichessApiProxy")
  public com.chessmate.external.api.lichess.LichessApi lichessApiProxy() {
    RestClient restClient = RestClient.builder()
        .baseUrl(lichessProperties.getBaseApiUrl())
        .defaultHeader("user-agent", "ChessLadder/1.0 (https://chessladder.org)")
        .defaultHeader("Accept", "application/json")
        .requestInterceptor((request, body, execution) -> {
          log.info("[Lichess API] 요청 경로: {} {}", request.getMethod(), request.getURI());
          log.info("[Lichess API] Authorization 헤더: {}",
              request.getHeaders().getFirst("Authorization") != null ? "Present" : "Not present");
          if (body != null && body.length > 0) {
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            log.info("[Lichess API] 요청 Body: {}", bodyStr);
          }
          ClientHttpResponse response = execution.execute(request, body);
          log.info("[Lichess API] 응답 상태: {}", response.getStatusCode());
          return response;
        })
        .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);

    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(adapter)
        .build();

    return factory.createClient(com.chessmate.external.api.lichess.LichessApi.class);
  }

}
