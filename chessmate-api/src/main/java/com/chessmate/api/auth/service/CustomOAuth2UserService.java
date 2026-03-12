package com.chessmate.api.auth.service;

import com.chessmate.api.auth.OAuth2Provider;
import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.auth.dto.ChesscomTokenResponse;
import com.chessmate.api.auth.dto.LichessTokenResponse;
import com.chessmate.api.auth.repository.OAuth2RedisRepository;
import com.chessmate.domain.lichess.user.LichessUser;
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
  private final LichessApiService lichessApiService;
  private final LichessUserRepositoryImpl lichessUserRepository;
  private final OAuth2RedisRepository redisRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    String Platform = userRequest.getClientRegistration().getRegistrationId();

    Map<String, Object> additionalParameters = userRequest.getAdditionalParameters();


    if ("lichess".equals(Platform)) {
      LichessTokenResponse tokenResponse = LichessTokenResponse.builder()
          .accessToken((String) additionalParameters.get("access_token"))
          .expiresIn((Long) additionalParameters.get("expires_in"))
          .tokenType((String) additionalParameters.get("token_type"))
          .build();

      LichessAccountDto account = lichessApiService.getUserAccount(tokenResponse.getAccessToken());

      LichessUser newluser = LichessUser.newUser(account.id(), account.username());

      LichessUser saveUser = lichessUserRepository.save(newluser);

      redisRepository.saveProviderToken(saveUser.getId(), OAuth2Provider.LICHESS, tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());

      return new UserPrincipal(saveUser.getId(), saveUser.getUsername(), OAuth2Provider.LICHESS);

    } else if ("chesscom".equals(Platform)) {
      ChesscomTokenResponse tokenResponse = ChesscomTokenResponse.builder()
          .accessToken((String) additionalParameters.get("access_token"))
          .refreshToken((String) additionalParameters.get("refresh_token"))
          .expiresIn((Long) additionalParameters.get("expires_in"))
          .idToken((String) additionalParameters.get("id_token"))
          .tokenType((String) additionalParameters.get("token_type"))
          .build();

      // 우선 id_token 을 디코딩하면 어떤 값이 나오는지 정확한걸 찾아봐야함 직접 해서.
      log.info("id_token: {}", tokenResponse.getIdToken());
      String idToken = tokenResponse.getIdToken();
      String[] parts = idToken.split("\\."); // JWT는 마침표(.)로 구분됨

      if (parts.length >= 2) {
        // 0: Header, 1: Payload, 2: Signature
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        log.info("조사된 Payload: {}", payload);
        // 출력 결과: {"sub":"12345", "name":"ChessPlayer", "iat":1516239022...}
      }
    }

    //여기서 해야할건 사용자 기본 정보를 요청해서 User db에 저장하는것인가?

    return null;
  }
}
