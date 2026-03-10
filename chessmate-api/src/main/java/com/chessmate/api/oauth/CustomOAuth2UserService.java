package com.chessmate.api.oauth;

import com.chessmate.infra_core.entity.OAuthPlatForm;
import com.chessmate.infra_core.entity.Profile;
import com.chessmate.infra_core.entity.User;
import com.chessmate.infra_core.repository.ProfileRepository;
import com.chessmate.infra_core.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * OAuth2 로그인 시 사용자 정보를 처리하는 서비스입니다.
 * Lichess와 Chess.com 플랫폼을 지원합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final RestTemplate restTemplate;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(request);
    Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
    String registrationId = request.getClientRegistration().getRegistrationId();

    Long profileId;

    if (registrationId.equals("lichess")) {
      profileId = handleLichessLogin(attributes).getId();
    } else if (registrationId.equals("chesscom")) {
      String username = (String) attributes.get("username");
      Map<String, Object> detailAttributes = fetchChessComPublicProfile(username);
      attributes.putAll(detailAttributes);
      profileId = handleChesscomLogin(attributes).getId();
    } else {
      throw new OAuth2AuthenticationException("Unsupported Provider");
    }

    return new OAuth2PrincipalDetails(profileId, attributes);
  }

  /**
   * Chess.com Public API에서 사용자 상세 정보를 조회합니다.
   *
   * @param username Chess.com 사용자명
   * @return API 응답 데이터
   */
  private Map<String, Object> fetchChessComPublicProfile(String username) {
    String url = "https://api.chess.com/pub/player/" + username;
    try {
      return restTemplate.getForObject(url, Map.class);
    } catch (Exception e) {
      log.error("Chess.com PubAPI 호출 실패: {}", username);
      return Collections.emptyMap();
    }
  }

  /**
   * Lichess 로그인 처리: User와 Profile을 생성 또는 업데이트합니다.
   *
   * @param attributes OAuth2 응답 데이터
   * @return 생성/업데이트된 Profile
   */
  private Profile handleLichessLogin(Map<String, Object> attributes) {
    String lichessId = (String) attributes.get("id");
    String username = (String) attributes.get("username");

    return profileRepository.findByPlatformIdAndPlatform(lichessId, OAuthPlatForm.LICHESS)
        .map(profile -> {
          // 기존 프로필 업데이트
          if (!username.equals(profile.getUsername())) {
            profile.setUsername(username);
            profile.setPlatformCreatedAt(LocalDateTime.now());
          }
          return profileRepository.save(profile);
        })
        .orElseGet(() -> {
          // 새 User 및 Profile 생성
          User newUser = userRepository.save(User.builder().build());
          Profile newProfile = Profile.builder()
              .user(newUser)
              .platform(OAuthPlatForm.LICHESS)
              .platformId(lichessId)
              .username(username)
              .platformCreatedAt(LocalDateTime.now())
              .build();
          return profileRepository.save(newProfile);
        });
  }

  /**
   * Chess.com 로그인 처리: User와 Profile을 생성 또는 업데이트합니다.
   *
   * @param attributes OAuth2 응답 데이터
   * @return 생성/업데이트된 Profile
   */
  private Profile handleChesscomLogin(Map<String, Object> attributes) {
    String playerId = String.valueOf(attributes.get("player_id"));
    String username = (String) attributes.get("username");

    return profileRepository.findByPlatformIdAndPlatform(playerId, OAuthPlatForm.CHESSCOM)
        .map(profile -> {
          // 기존 프로필 업데이트
          if (!username.equals(profile.getUsername())) {
            profile.setUsername(username);
            profile.setPlatformCreatedAt(LocalDateTime.now());
          }
          return profileRepository.save(profile);
        })
        .orElseGet(() -> {
          // 새 User 및 Profile 생성
          User newUser = userRepository.save(User.builder().build());
          Profile newProfile = Profile.builder()
              .user(newUser)
              .platform(OAuthPlatForm.CHESSCOM)
              .platformId(playerId)
              .username(username)
              .platformCreatedAt(LocalDateTime.now())
              .build();
          return profileRepository.save(newProfile);
        });
  }
}