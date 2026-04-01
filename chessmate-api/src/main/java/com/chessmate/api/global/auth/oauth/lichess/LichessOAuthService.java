package com.chessmate.api.global.auth.oauth.lichess;

import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.api.global.auth.jwt.JwtService;
import com.chessmate.infra_redis.repository.OAuth2RedisRepository;
import com.chessmate.api.global.auth.oauth.common.PlatFormOAuthService;
import com.chessmate.api.global.auth.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.api.redis.GameTaskProducer;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.external.api.lichess.LichessApi;
import com.chessmate.external.dto.OAuthUrlInfoDTO;
import com.chessmate.external.dto.account.LichessAccountDto;
import com.chessmate.external.dto.lichess.LichessTokenResponse;
import com.chessmate.external.service.OAuthService;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_redis.repository.LichessRedisRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Lichess OAuth 서비스
 * Lichess 플랫폼의 OAuth 인증 URL 생성 및 PKCE 검증 코드 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LichessOAuthService implements PlatFormOAuthService {

  private final OAuth2RedisRepository oAuth2RedisRepository;
  private final OAuthService oAuthService;
  private final GameTaskProducer gameTaskProducer;
  private final LichessApi lichessApi;
  private final JwtService jwtService;
  private final LichessUserRepository lichessUserRepository;
  private final LichessRedisRepository lichessRedisRepository;

  /**
   * Lichess OAuth URL 생성
   *
   * @return OAuthUrlResponse - OAuth URL과 플랫폼 정보
   */
  @Override
  public OAuthUrlResponse getOAuthUrl() {
    OAuthUrlInfoDTO info = oAuthService.generateOauthUrl(OAuthPlatForm.LICHESS);

    // Redis에 K : state V : code_verifier 저장 추후 토큰 발급때 꺼내서 사용 ttl: 100초
    oAuth2RedisRepository.saveCodeVerifier(info.state(), info.codeVerifier(), 100);

    return new OAuthUrlResponse(OAuthPlatForm.LICHESS, info.oauthUrl());
  }

  @Override
  public TokenResponse callback(String code, String state) {
    // Redis에서 code_verifier 조회
    String codeVerifier = oAuth2RedisRepository.getCodeVerifier(state);

    // code_verifier 검증: TTL 만료 또는 잘못된 state인 경우
    if (codeVerifier == null) {
      log.warn("[OAuth Callback] Code verifier not found or expired - state={}", state);
      oAuth2RedisRepository.deleteCodeVerifier(state);
      throw new AuthException(AuthErrorCode.OAUTH_CODE_VERIFIER_NOT_FOUND);
    }
    // code_verifier 제거
    oAuth2RedisRepository.deleteCodeVerifier(state);

    try {
      // 1. Chesscom에서 토큰 발급
      LichessTokenResponse tokenResponse = oAuthService.getLichessToken(code, codeVerifier);

      String bearerToken = tokenResponse.getTokenType() + " " + tokenResponse.getAccessToken();
      LichessAccountDto account = lichessApi.getCurrentAccount(bearerToken);

      // 3. 기존 사용자 조회 또는 신규 사용자 생성
      LichessUser lichessUser = lichessUserRepository.findByLichessId(account.id())
          .orElseGet(() -> LichessUser.builder()
              .id(null)
              .lichessId(account.id())
              .banner(null)
              .profile(null)
              .username(account.username())
              .createdAt(LocalDateTime.now())
              .description(null)
              .build());


      boolean isNewUser = lichessUser.getId() == null;

      LichessUser saveUser = lichessUserRepository.save(lichessUser);


      lichessRedisRepository.saveAccessToken(saveUser.getLichessId(), tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());
      log.info("[OAuth Callback] lichessId={} accesstoken={} expiresIn={}", saveUser.getLichessId(), tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());

      // 4. 새로운 사용자인 경우 게임 동기화 작업 큐에 추가
      if (isNewUser) {
        gameTaskProducer.enqueueNewUserGameSync(
            OAuthPlatForm.CHESSCOM,
            saveUser.getUsername(),
            saveUser.getId()
        );
      }

      TokenResponse response = jwtService.generateTokenResponse(saveUser.getId(), OAuthPlatForm.CHESSCOM, saveUser.getLichessId());

      return response;

    } catch (AuthException authException) {
      log.error("[OAuth Callback] Auth Exception - {}", authException.getMessage());
      throw authException;
    } catch (Exception exception) {
      log.error("[OAuth Callback] Unexpected Exception - {}", exception.getMessage(), exception);
      throw new AuthException(AuthErrorCode.OAUTH_CALLBACK_FAILED);
    }

  }
}



