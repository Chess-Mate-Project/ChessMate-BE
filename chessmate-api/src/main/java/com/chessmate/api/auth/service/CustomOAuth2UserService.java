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

      return new UserPrincipal(luser.getId(),  OAuth2Provider.LICHESS);

    } else if ("chesscom".equals(Platform)) {
      // Chess.com OIDC 토큰 정보 추출
      ChesscomTokenResponse tokenResponse = ChesscomTokenResponse.builder()
          .accessToken(userRequest.getAccessToken().getTokenValue())
          .tokenType(userRequest.getAccessToken().getTokenType().getValue())
          .expiresIn(userRequest.getAccessToken().getExpiresAt() != null ?
              userRequest.getAccessToken().getExpiresAt().getEpochSecond() : 3600L)
          .idToken((String) additionalParameters.get("id_token"))
          .refreshToken((String) additionalParameters.get("refresh_token"))
          .build();

      log.info("==== [Chess.com OAuth2 Token Response Debug] ====");
      log.info("Access Token: {}", tokenResponse.getAccessToken().substring(0, Math.min(20, tokenResponse.getAccessToken().length())) + "...");
      log.info("Token Type: {}", tokenResponse.getTokenType());
      log.info("Expires In: {}", tokenResponse.getExpiresIn());
      log.info("Has ID Token: {}", tokenResponse.getIdToken() != null);
      log.info("Has Refresh Token: {}", tokenResponse.getRefreshToken() != null);

      // id_token에서 사용자 정보 추출 (JWT 디코딩)
      String idToken = tokenResponse.getIdToken();
      if (idToken == null || idToken.isEmpty()) {
        log.error("id_token이 없습니다. Chess.com에서 openid scope을 제대로 받지 못했을 가능성이 있습니다.");
        throw new OAuth2AuthenticationException("id_token이 없습니다");
      }

      String[] parts = idToken.split("\\.");
      if (parts.length < 2) {
        log.error("Invalid JWT format");
        throw new OAuth2AuthenticationException("Invalid JWT format");
      }

      try {
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        log.info("id_token Payload: {}", payload);

        // JSON 파싱 로직은 ObjectMapper 사용 가능 (필요시)
        // 여기서는 sub 필드만 추출해서 사용자 ID로 사용
      } catch (IllegalArgumentException e) {
        log.error("Failed to decode JWT payload", e);
        throw new OAuth2AuthenticationException("Failed to decode JWT");
      }

      // attributes에서 사용자 정보 추출
      Map<String, Object> attributes = userRequest.getAdditionalParameters();
      log.info("==== [Chess.com Additional Parameters] ====");
      attributes.forEach((key, value) -> log.info("{}: {}", key, value));

      // TODO: ChessComUser 엔티티 조회/생성 및 저장
      // 현재는 임시로 UserPrincipal 반환

      redisRepository.saveProviderToken(
          999L, // 임시 ID (나중에 ChessComUser 저장 후 ID로 변경)
          OAuth2Provider.CHESSCOM,
          tokenResponse.getAccessToken(),
          Math.toIntExact(tokenResponse.getExpiresIn())
      );

      return new UserPrincipal(999L, OAuth2Provider.CHESSCOM);
    }

    log.error("Unknown OAuth2 platform: {}", Platform);
    throw new OAuth2AuthenticationException("Unknown OAuth2 platform: " + Platform);
  }
}
