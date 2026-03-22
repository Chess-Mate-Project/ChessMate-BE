package com.chessmate.api.oauth.chesscom;

import com.chessmate.api.global.auth.repository.OAuth2RedisRepository;
import com.chessmate.api.oauth.common.PlatFormOAuthService;
import com.chessmate.api.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.external.config.ChesscomProperties;
import com.chessmate.external.dto.OAuthUrlInfoDTO;
import com.chessmate.external.oauth.chesscom.ChesscomOauthApi;
import com.chessmate.external.service.OAuthService;
import com.chessmate.external.type.OAuthPlatForm;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Chess.com OAuth 서비스
 * Chess.com 플랫폼의 OAuth 인증 URL 생성 및 PKCE 검증 코드 관리
 */
@Service
@RequiredArgsConstructor
public class ChesscomOAuthService implements PlatFormOAuthService {

  private final OAuth2RedisRepository oAuth2RedisRepository;
  private final OAuthService oAuthService;

  /**
   * Chess.com OAuth URL 생성
   *
   * @return OAuthUrlResponse - OAuth URL과 플랫폼 정보
   */
  @Override
  public OAuthUrlResponse getOAuthUrl() {
    OAuthUrlInfoDTO info = oAuthService.generateOauthUrl(OAuthPlatForm.CHESSCOM);

    // Redis에 K : state V : code_verifier 저장 추후 토큰 발급때 꺼내서 사용 ttl: 100초
    oAuth2RedisRepository.saveCodeVerifier(info.state(), info.codeVerifier(), 100);

    return new OAuthUrlResponse(OAuthPlatForm.CHESSCOM, info.oauthUrl());
  }

  @Override
  public void callback(String code, String state) {
    // Redis에서 code_verifier 조회
    String codeVerifier = oAuth2RedisRepository.getCodeVerifier(state);

    // code_verifier 제거
    oAuth2RedisRepository.deleteCodeVerifier(state);

    Map<String, Object> tokenResponse =  oAuthService.getTokenApiResponse(OAuthPlatForm.CHESSCOM, code, codeVerifier);

    // tokenResponse 내부값 모두 출력
    System.out.println("=== Chess.com Token Response ===");
    tokenResponse.forEach((key, value) -> {
      System.out.println("Key: " + key + ", Value: " + value + ", Type: " + (value != null ? value.getClass().getSimpleName() : "null"));
    });
    System.out.println("================================");

  }

  

}
