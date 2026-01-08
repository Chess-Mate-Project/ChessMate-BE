package com.chessmate.external.service;

import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.external.config.LichessConfig;
import com.chessmate.external.dto.account.LichessAccountDto;
import com.chessmate.external.dto.game.LichessGamesDto;
import com.chessmate.external.dto.oauth.OAuthValueRequest;
import com.chessmate.external.dto.oauth.OauthAccessTokenDto;
import jdk.jshell.spi.ExecutionControl.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class LichessApiService {

  private final LichessConfig lichessConfig;
  private final WebClient webClient;


  /*
  * [ Lichess OAuth Access Token 발급 요청 ]
  *  - 설명: Lichess OAuth 서버에 Authorization Code와 Code Verifier를 포함한
  *    요청을 보내어 OAuth Access Token을 발급받음
  *
  * - Parameters:
  *   - request: OAuthValueRequest 객체로, Authorization Code와 Code Verifier를 포함
  * - Returns: OAuthAccessTokenResponse 객체
  * - Throws: AuthException - OAuth Access Token 발급 실패 시 발생
  * */

  public OauthAccessTokenDto getOAuthAccessToken(OAuthValueRequest request) {
    return webClient.post()
        .uri("/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters
            .fromFormData("grant_type", "authorization_code")
            .with("code", request.code())
            .with("client_id", lichessConfig.getClientId())
            .with("redirect_uri", lichessConfig.getRedirectUrl())
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

  /**
  * - [ Lichess 사용자 계정 정보 조회 ]
   * * - 설명: Lichess API를 통해 사용자 계정 정보를 조회
   * - @param token Lichess OAuth Access Token
   * - @return LichessAccountDto Lichess 사용자 계정 정보 DTO
   * - Throws: UserException - 사용자 계정 정보 조회 실패 시 발생
  **/
  public LichessAccountDto getUserAccount(String token) {
    return webClient.get()
        .uri("/account")
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .bodyToMono(LichessAccountDto.class)
        .doOnError(e -> {
          throw new com.chessmate.common.exception.UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT);
        }).block();
  }

  /**
   * - [ Lichess 사용자 게임 기록 조회 (Reactive) ]
   * - 설명: Lichess API를 통해 특정 사용자의 게임 기록을 Reactive 방식으로 조회
   * - @param token Lichess OAuth Access Token
   * - @param username 조회할 Lichess 사용자 이름
   * - @return Flux<LichessGamesDto> Lichess 사용자 게임 기록 DTO의 Flux 스트림
   * - Throws: UserException - 사용자 게임 기록 조회 실패 시 발생
   * */
  public Flux<LichessGamesDto> getUserGamesReactive(String token, String username) {
    log.info("username = " + "matteorf2b");
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/games/user/{username}")
            .queryParam("perf", "rapid,bullet,classical,blitz")
            .queryParam("opening", "true")
            .build("matteorf2b"))
        .accept(MediaType.parseMediaType("application/x-ndjson"))
        .headers(h -> h.setBearerAuth(token))
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, resp ->
            resp.bodyToMono(String.class)
                .doOnNext(body -> log.error("4xx 오류 응답 바디: {}", body))
                .then(Mono.error(new com.chessmate.common.exception.UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT)))
        )
        .bodyToFlux(LichessGamesDto.class);
  }



}
