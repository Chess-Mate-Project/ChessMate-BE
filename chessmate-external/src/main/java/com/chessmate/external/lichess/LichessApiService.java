package com.chessmate.external.lichess;

import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.common.exception.UserException;
import com.chessmate.common.type.GameType;
import com.chessmate.external.lichess.dto.account.LichessAccountDto;
import com.chessmate.external.lichess.dto.game.LichessGamesDto;
import com.chessmate.external.lichess.dto.oauth.OAuthValueRequest;
import com.chessmate.external.lichess.dto.oauth.OauthAccessTokenDto;
import com.chessmate.external.lichess.dto.perf.UserPerfDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class LichessApiService {

  private final LichessConfig lichessConfig;
  private final WebClient webClient;

  public LichessApiService(LichessConfig lichessConfig, @Qualifier("lichessWebClient") WebClient webClient) {
    this.lichessConfig = lichessConfig;
    this.webClient = webClient;
  }

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
        .uri(lichessConfig.getTokenUrl())
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
        .headers(headers -> {
          headers.setBearerAuth(token);
          headers.set("user-agent", "ChessLadder/1.0 (https://chessladder.org)");
        })
        .retrieve()
        .bodyToMono(LichessAccountDto.class)
        .doOnError(e -> {
          throw new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT);
        }).block();
  }



  /**
   * - [ Lichess 사용자 게임 기록 조회 (Reactive) ]
   * - 설명: Lichess API를 통해 특정 사용자의 게임 기록을 Reactive 방식으로 조회
   * - @param token Lichess OAuth Access Token
   * - @param username 조회할 Lichess 사용자 이름
   * - @param since 조회 시작 시간 (밀리초, null이면 0)
   * - @param until 조회 종료 시간 (밀리초)
   * - @return Flux<LichessGamesDto> Lichess 사용자 게임 기록 DTO의 Flux 스트림
   * - Throws: UserException - 사용자 게임 기록 조회 실패 시 발생
   * */
  public Flux<LichessGamesDto> getUserGamesReactive(String token, String username, Long since, Long until) {
    log.info("[LichessAPI] 게임 조회 시작 - username={}, since={}, until={}", username, since, until);

    return webClient.get()
        .uri(uriBuilder -> {
          var builder = uriBuilder
              .path("/games/user/{username}")
              .queryParam("perf", "rapid,bullet,classical,blitz")
              .queryParam("opening", "true")
              .queryParam("rated", "true");
          
          // since값이 있음 -> 증분 추가
          // since 값이 없음 -> 초기 가입 전체 동기화함
          if (since != null && since > 0) {
            builder.queryParam("since", since);
          }

          // until 값으로 조회 범위의 끝을 명시적으로 지정
          // 이를 통해 최근 데이터까지 정확하게 조회 가능
          if (until != null && until > 0) {
            builder.queryParam("until", until);
          }

          return builder.build(username);
        })
        .accept(MediaType.parseMediaType("application/x-ndjson"))
        .headers(h -> {
          h.setBearerAuth(token);
          h.set("user-agent", "ChessLadder/1.0 (https://chessladder.org)");
        })
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, resp ->
            resp.bodyToMono(String.class)
                .doOnNext(body -> log.error("[LichessAPI] 4xx 오류 응답 바디: {}", body))
                .then(Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT)))
        )
        .bodyToFlux(LichessGamesDto.class);
  }

  /**
   * - [ Lichess 사용자 게임 통계 조회 ]
   * - 설명: Lichess API를 통해 특정 사용자의 게임 타입별 통계를 조회
   * - 엔드포인트: GET /api/user/{username}/perf/{perfType}
   * - @param username 조회할 Lichess 사용자 이름
   * @param gameType 게임 타입 (bullet, blitz, rapid, classical, ultraBullet 등)
   * - @return UserPerfDto 사용자 게임 통계 정보
   * - Throws: UserException - 사용자 게임 통계 조회 실패 시 발생
   * */
  public UserPerfDto getUserPerf(String username, GameType gameType) {
    log.info("[LichessAPI] 게임 통계 조회 시작 - username={}, gameType={}", username, gameType);

    try {
      UserPerfDto result = webClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/user/{username}/perf/{perfType}")
              .build(username, gameType.name().toLowerCase()))
          .headers(h -> h.set("user-agent", "ChessLadder/1.0 (https://chessladder.org)"))
          .retrieve()
          .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
            return clientResponse.bodyToMono(String.class)
                .doOnNext(errorBody -> log.error("[LichessAPI] 4xx 오류 응답 - username={}, gameType={}, body={}",
                    username, gameType, errorBody))
                .then(Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT)));
          })
          .onStatus(HttpStatusCode::is5xxServerError, serverResponse -> {
            return serverResponse.bodyToMono(String.class)
                .doOnNext(errorBody -> log.error("[LichessAPI] 5xx 오류 응답 - username={}, gameType={}, body={}",
                    username, gameType, errorBody))
                .then(Mono.error(new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT)));
          })
          .bodyToMono(UserPerfDto.class)
          .doOnNext(result2 -> log.info("[LichessAPI] 게임 통계 조회 완료 - username={}, gameType={}, rating={}, all={}",
              username, gameType, result2.perf().glicko().rating(), result2.stat().count().all()))
          .block();

      if (result == null) {
        log.error("[LichessAPI] UserPerfDto가 null - username={}, gameType={}", username, gameType);
        throw new UserException(UserErrorCode.FAILD_GET_USER_ACCOUNT);
      }

      return result;

    } catch (Exception e) {
      log.error("[LichessAPI] 게임 통계 조회 실패 - username={}, gameType={}, error={}",
          username, gameType, e.getMessage(), e);
      throw e;
    }
  }

}
