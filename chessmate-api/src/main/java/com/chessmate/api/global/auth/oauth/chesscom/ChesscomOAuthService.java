package com.chessmate.api.global.auth.oauth.chesscom;

import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.api.global.auth.jwt.JwtService;
import com.chessmate.infra_redis.repository.AuthRedisRepository;
import com.chessmate.infra_redis.repository.OAuth2RedisRepository;
import com.chessmate.infra_redis.sync.SyncJobProducer;
import com.chessmate.api.global.auth.oauth.common.PlatFormOAuthService;
import com.chessmate.api.global.auth.oauth.common.dto.OAuthUrlResponse;
import com.chessmate.domain.chesscom.user.ChesscomUser;
import com.chessmate.domain.sync.SyncJob;
import com.chessmate.domain.sync.SyncJobRepository;
import com.chessmate.external.api.chesscom.ChesscomApi;
import com.chessmate.external.dto.chesscom.ChesscomPublicProfileResponse;
import com.chessmate.external.dto.chesscom.ChesscomTokenResponse;
import com.chessmate.external.dto.OAuthUrlInfoDTO;
import com.chessmate.external.dto.chesscom.ChesscomUserInfo;
import com.chessmate.external.service.OAuthService;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.exception.AuthException;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chess.com OAuth 서비스
 * Chess.com 플랫폼의 OAuth 인증 URL 생성 및 PKCE 검증 코드 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChesscomOAuthService implements PlatFormOAuthService {

  private final ChesscomUserRepository chesscomUserRepository;
  private final OAuth2RedisRepository oAuth2RedisRepository;
  private final OAuthService oAuthService;
  private final ChesscomUtil chesscomUtil;
  private final SyncJobProducer syncJobProducer;
  private final SyncJobRepository syncJobRepository;
  private final JwtService jwtService;
  private final AuthRedisRepository authRedisRepository;
  private final ChesscomApi chesscomApi;


  /**
   * Chess.com OAuth URL 생성
   *
   * @return OAuthUrlResponse - OAuth URL과 플랫폼 정보
   */
  @Override
  public OAuthUrlResponse getOAuthUrl() {
    OAuthUrlInfoDTO info = oAuthService.generateOauthUrl(OAuthPlatForm.CHESSCOM);

    oAuth2RedisRepository.saveCodeVerifier(info.state(), info.codeVerifier(), 100);

    return new OAuthUrlResponse(OAuthPlatForm.CHESSCOM, info.oauthUrl());
  }

  @Override
  @Transactional
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
      ChesscomTokenResponse tokenResponse = oAuthService.getChesscomToken(code, codeVerifier);

      // 2. ID Token 파싱하여 사용자 정보 추출
      ChesscomUserInfo chesscomUserInfo = chesscomUtil.parseIdToken(tokenResponse.getIdToken());

      // 3. Chess.com 공개 프로필에서 가입일 조회
      LocalDateTime chesscomJoinedAt = null;
      try {
          ChesscomPublicProfileResponse profile = chesscomApi.getPlayerProfile(chesscomUserInfo.getUsername());
          if (profile.joined() != null && profile.joined() > 0) {
              chesscomJoinedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(profile.joined()), ZoneOffset.UTC);
          }
      } catch (Exception e) {
          log.warn("[OAuth Callback] Chess.com 공개 프로필 조회 실패 (가입일 없음) username={} error={}",
              chesscomUserInfo.getUsername(), e.getMessage());
      }
      final LocalDateTime finalJoinedAt = chesscomJoinedAt;

      // 4. 기존 사용자 조회 또는 신규 사용자 생성
      ChesscomUser chesscomUser = chesscomUserRepository.findByChesscomId(Long.valueOf(chesscomUserInfo.getUserId()))
              .orElseGet(() -> ChesscomUser.builder()
                    .id(null)
                    .chesscomId(Long.valueOf(chesscomUserInfo.getUserId()))
                    .profile(null)
                    .banner(null)
                    .description(null)
                    .username(chesscomUserInfo.getUsername())
                    .createdAt(LocalDateTime.now())
                    .platformJoinedAt(finalJoinedAt)
                    .build());

      boolean isNewUser = chesscomUser.getId() == null;
      boolean isRestored = !isNewUser && chesscomUser.isDeleted();

      if (isRestored) {
        chesscomUser.restore(chesscomUserInfo.getUsername());
        log.info("[OAuth Callback] Chess.com 탈퇴 계정 복원 chesscomId={}", chesscomUserInfo.getUserId());
      }

      ChesscomUser saveUser = chesscomUserRepository.save(chesscomUser);
      authRedisRepository.saveChesscomAccessToken(saveUser.getId(), tokenResponse.getAccessToken(), tokenResponse.getExpiresIn());
      authRedisRepository.saveChesscomRefreshToken(saveUser.getId(), tokenResponse.getRefreshToken(), 30 * 24 * 3600);

      // 5. 신규 또는 복원 사용자의 경우 SyncJob 생성 후 큐 등록
      if (isNewUser || isRestored) {
        SyncJob syncJob = SyncJob.create(saveUser.getId(), OAuthPlatForm.CHESSCOM, chesscomUserInfo.getUsername());
        SyncJob savedJob = syncJobRepository.save(syncJob);
        syncJobProducer.enqueue(OAuthPlatForm.CHESSCOM, savedJob.getId());
        log.info("[OAuth Callback] Chess.com SyncJob 등록 userId={} jobId={}", saveUser.getId(), savedJob.getId());
      }

      TokenResponse response = jwtService.generateTokenResponse(saveUser.getId(), OAuthPlatForm.CHESSCOM, String.valueOf(saveUser.getChesscomId()));

      return response;

    } catch (AuthException authException) {
      log.error("[OAuth Callback] Auth Exception - {}", authException.getMessage());
      throw authException;
    } catch (Exception exception) {
      log.error("[OAuth Callback] Unexpected Exception - {}", exception.getMessage(), exception);
      throw new AuthException(AuthErrorCode.OAUTH_CALLBACK_FAILED);
    }
  }

  /**
   * Refresh Token을 사용하여 새로운 Access Token 발급
   *
   * @param refreshToken 기존 Refresh Token
   * @return 새로운 토큰 정보 (idToken, accessToken, refreshToken 등)
   */
  @Transactional(readOnly = true)
  public ChesscomTokenResponse refreshAccessToken(String refreshToken) {
    try {
      log.info("[OAuth Refresh] Attempting to refresh access token");

      ChesscomTokenResponse tokenResponse = oAuthService.refreshChesscomToken(refreshToken);

      log.info("[OAuth Refresh] Access token refreshed successfully");
      log.info("Token Type: {}", tokenResponse.getTokenType());
      log.info("Expires In: {} seconds", tokenResponse.getExpiresIn());

      return tokenResponse;
    } catch (AuthException authException) {
      log.error("[OAuth Refresh] Auth Exception - {}", authException.getMessage());
      throw authException;
    } catch (Exception exception) {
      log.error("[OAuth Refresh] Unexpected Exception - {}", exception.getMessage(), exception);
      throw new AuthException(AuthErrorCode.OAUTH_REFRESH_FAILED);
    }
  }

}


