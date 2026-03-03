package com.chessmate.external.service;

import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.external.config.ChesscomConfig;
import com.chessmate.external.config.LichessConfig;
import com.chessmate.external.dto.oauth.OAuthValueRequest;
import com.chessmate.external.dto.oauth.OauthAccessTokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ChesscomApiService {

  private final ChesscomConfig chesscomConfig;
  private final WebClient webClient;

  public ChesscomApiService(ChesscomConfig chesscomConfig, @Qualifier("chesscomWebClient") WebClient webClient) {
    this.chesscomConfig = chesscomConfig;
    this.webClient = webClient;
  }

  public OauthAccessTokenDto getOAuthAccessToken(OAuthValueRequest request) {
    return webClient.post()
        .uri(chesscomConfig.getTokenUrl())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .headers(headers -> headers.set("user-agent", "ChessLadder/1.0 (https://chessladder.org)"))
        .body(BodyInserters
            .fromFormData("grant_type", "authorization_code")
            .with("code", request.code())
            .with("client_id", chesscomConfig.getClientId())
            .with("redirect_uri", chesscomConfig.getRedirectUrl())
            .with("code_verifier", request.codeVerifier()))
        .retrieve()

        .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
          return clientResponse.bodyToMono(String.class)
              .doOnNext(errorBody -> log.error("4xx 오류 응답 바디: {}", errorBody))
              .then(Mono.error(new AuthException(AuthErrorCode.FAILD_GET_OAUTH_ACCESS_TOKEN)));
        })

        .bodyToMono(OauthAccessTokenDto.class)
        .block();
  }

}