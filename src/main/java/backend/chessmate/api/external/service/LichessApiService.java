package backend.chessmate.api.external.service;

import backend.chessmate.api.external.config.LichessConfig;
import backend.chessmate.api.external.dto.account.LichessAccountDto;
import backend.chessmate.api.oauth.dto.OauthAccessTokenDto;
import backend.chessmate.api.oauth.dto.request.OAuthValueRequest;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.code.UserErrorCode;
import backend.chessmate.global.common.exception.AuthException;
import backend.chessmate.global.common.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
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
              .doOnNext(errorBody -> log.error("❌ 4xx 오류 응답 바디: {}", errorBody))
              .then(Mono.error(new AuthException(AuthErrorCode.FAILD_GET_OAUTH_ACCESS_TOKEN)));
        })


        .bodyToMono(OauthAccessTokenDto.class)
        .block();
  }

      public LichessAccountDto getUserAccount(String token) {
        log.info(token + "토큰");
        return webClient.get()
                .uri("/account")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(LichessAccountDto.class)
                .doOnError(e -> {
                    throw new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT);
                }).block();

    }

}
