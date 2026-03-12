package com.chessmate.api.auth.service;

import com.chessmate.api.auth.OAuth2Provider;
import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.auth.dto.ChesscomTokenResponse;
import com.chessmate.api.auth.dto.LichessTokenResponse;
import com.chessmate.api.auth.repository.OAuth2RedisRepository;
import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.external.config.LichessApi;
import com.chessmate.external.dto.account.LichessAccountDto;
import com.chessmate.external.service.LichessApiService;
import com.chessmate.infra_persistence.lichess.user.repositoryImpl.LichessUserRepositoryImpl;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

  private final OAuth2RedisRepository oauth2RedisRepository;
  private final LichessApi lichessApi;
  private final LichessUserRepositoryImpl lichessUserRepository;
  private final OAuth2RedisRepository redisRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    String Platform = userRequest.getClientRegistration().getRegistrationId();

    Map<String, Object> additionalParameters = userRequest.getAdditionalParameters();

    if ("lichess".equals(Platform)) {
      LichessTokenResponse tokenResponse = LichessTokenResponse.builder()
          .accessToken(userRequest.getAccessToken().getTokenValue())
          .tokenType(userRequest.getAccessToken().getTokenType().getValue())
          .expiresIn(userRequest.getAccessToken().getExpiresAt().getNano()) // 만료시간 계산 로직 필요
          .build();


      log.info("==== [OAuth2 Token Response Debug] Platform: {} ====", Platform);

      log.info("Access Token: {}", userRequest.getAccessToken().getTokenValue());
      log.info("Expires In: {}", userRequest.getAccessToken().getTokenType().getValue());
      log.info("Token Type: {}", userRequest.getAccessToken().getExpiresAt().getNano());

      log.info("==========================================================");
      LichessAccountDto account = lichessApi.getMyAccount(
          tokenResponse.getTokenType() + " " +tokenResponse.getAccessToken()
      );
      LichessUser luser = lichessUserRepository.findByLichessId(account.id())
          .map(existingUser -> { // -> 이미 존재하는 사용자일 때 user가 바뀌었을 수 있으니 업데이트
            existingUser.setUsername(account.username());
            return lichessUserRepository.save(existingUser);
          })
          .orElseGet(() -> { // -> 신규 사용자라면 새로 저장
            LichessUser newUser = LichessUser.newUser(account.id(), account.username());
            return lichessUserRepository.save(newUser);
          });


      /**
       * -> Lichess API에서 사용자 정보를 가져온 후, 사용자 ID와 토큰 정보를 Redis에 저장
       *  key : id를 포함한 prefix / value : accessToken, / ttl : accessToken의 만료시간
       * **/
      redisRepository.saveProviderToken(
          luser.getId(),
          OAuth2Provider.LICHESS,
          tokenResponse.getAccessToken(),
          tokenResponse.getExpiresIn()
      );

      return new UserPrincipal(luser.getId(), luser.getUsername(), OAuth2Provider.LICHESS);

    } else if ("chesscom".equals(Platform)) {
//      ChesscomTokenResponse tokenResponse = ChesscomTokenResponse.builder()
//          .accessToken(userRequest.getAccessToken().getTokenValue())
//          .tokenType(userRequest.getAccessToken().getTokenType().getValue())
//          .refreshToken(userRequest.getRefreshToken() != null ? userRequest.getRefreshToken().getTokenValue() : null)
//          // id_token 같은 '비표준'만 여기서 꺼낸다!
//          .idToken((String) userRequest.getAdditionalParameters().get("id_token"))
//          .build();
      log.info("==== [OAuth2 Token Response Debug] Platform: {} ====", Platform);

      log.info("Access Token: {}", additionalParameters.get("access_token"));
      log.info("Refresh Token: {}", additionalParameters.get("refresh_token"));
      log.info("Expires In: {}", additionalParameters.get("expires_in"));
      log.info("ID Token: {}", additionalParameters.get("id_token"));
      log.info("Token Type: {}", additionalParameters.get("token_type"));

      log.info("==========================================================");

//      // 우선 id_token 을 디코딩하면 어떤 값이 나오는지 정확한걸 찾아봐야함 직접 해서.
//      log.info("id_token: {}", tokenResponse.getIdToken());
//      String idToken = tokenResponse.getIdToken();
//      String[] parts = idToken.split("\\."); // JWT는 마침표(.)로 구분됨
//
//      if (parts.length >= 2) {
//        // 0: Header, 1: Payload, 2: Signature
//        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
//        log.info("조사된 Payload: {}", payload);
//        // 출력 결과: {"sub":"12345", "name":"ChessPlayer", "iat":1516239022...}
//      }
    }

    //여기서 해야할건 사용자 기본 정보를 요청해서 User db에 저장하는것인가?

    return null;
  }
}
