package com.chessmate.api.oauth;

import com.chessmate.infra_core.entity.ChesscomProfile;
import com.chessmate.infra_core.entity.LichessProfile;
import com.chessmate.infra_core.repository.ChesscomProfileRepository;
import com.chessmate.infra_core.repository.LichessProfileRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final LichessProfileRepository lichessProfileRepository;
  private final ChesscomProfileRepository chesscomProfileRepository;
  private final RestTemplate restTemplate; // 추가 API 호출용

  @Override
  public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(request);
    Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
    String registrationId = request.getClientRegistration().getRegistrationId();

    Long profileId;

    if (registrationId.equals("lichess")) {
      profileId = handleLichessLogin(attributes).getId();
    } else if (registrationId.equals("chesscom")) {
      // 1. attributes나 id_token에서 username 추출 (Chess.com 설정에 따라 다름)
      String username = (String) attributes.get("username");

      // 2. Username이 확보되었으니 PubAPI로 상세 정보 조회
      Map<String, Object> detailAttributes = fetchChessComPublicProfile(username);
      attributes.putAll(detailAttributes); // 기존 정보와 합침

      profileId = handleChesscomLogin(attributes).getId();
    } else {
      throw new OAuth2AuthenticationException("Unsupported Provider");
    }

    return new OAuth2PrincipalDetails(profileId, attributes);
  }

  private Map<String, Object> fetchChessComPublicProfile(String username) {
    String url = "https://api.chess.com/pub/player/" + username;
    try {
      return restTemplate.getForObject(url, Map.class);
    } catch (Exception e) {
      log.error("Chess.com PubAPI 호출 실패: {}", username);
      return Collections.emptyMap();
    }
  }

  private LichessProfile handleLichessLogin(Map<String, Object> attributes) {
    String lichessId = (String) attributes.get("id");
    String username = (String) attributes.get("username");

    return lichessProfileRepository.findByLichessId(lichessId)
        .map(profile -> {
          if (!username.equals(profile.getUsername())) {
            profile.setUsername(username);
            profile.setUpdatedAt(LocalDateTime.now());
          }
          return lichessProfileRepository.save(profile);
        })
        .orElseGet(() -> lichessProfileRepository.save(
            LichessProfile.builder()
                .lichessId(lichessId)
                .username(username)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        ));
  }

  // handleChessComLogin 로직 내에서 attributes.get("player_id") 등을 활용해 DB 저장
  public ChesscomProfile handleChesscomLogin(Map<String, Object> attributes) {
    String chesscomId = (String) attributes.get("player_id");
    String username = (String) attributes.get("username");

    return  null;
  }
}