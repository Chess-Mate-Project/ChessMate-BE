package com.chessmate.api.global.auth.service;

import com.chessmate.api.global.auth.dto.MeResponse;
import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.global.auth.jwt.JwtService;
import com.chessmate.api.global.auth.oauth.common.CookieManager;
import com.chessmate.api.global.auth.oauth.common.CookieName;
import com.chessmate.api.global.auth.service.strategy.LogoutStrategy;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.code.UserErrorCode;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.exception.AuthException;
import com.chessmate.common.exception.UserException;
import com.chessmate.api.image.ImageUtil;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.infra_redis.repository.AuthRedisRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

  private final JwtService jwtService;
  private final AuthRedisRepository authRedisRepository;
  private final CookieManager cookieManager;
  private final LichessUserRepository lichessUserRepository;
  private final ChesscomUserRepository chesscomUserRepository;
  private final ImageUtil imageUtil;
  private final Map<OAuthPlatForm, LogoutStrategy> logoutStrategyMap;

  public AuthService(
      JwtService jwtService,
      AuthRedisRepository authRedisRepository,
      CookieManager cookieManager,
      LichessUserRepository lichessUserRepository,
      ChesscomUserRepository chesscomUserRepository,
      ImageUtil imageUtil,
      List<LogoutStrategy> strategies
  ) {
    this.jwtService = jwtService;
    this.authRedisRepository = authRedisRepository;
    this.cookieManager = cookieManager;
    this.lichessUserRepository = lichessUserRepository;
    this.chesscomUserRepository = chesscomUserRepository;
    this.imageUtil = imageUtil;
    this.logoutStrategyMap = strategies.stream()
        .collect(Collectors.toMap(LogoutStrategy::getProvider, s -> s));
  }

  public void logout(UserPrincipal userPrincipal, HttpServletResponse res) {
    LogoutStrategy strategy = logoutStrategyMap.get(userPrincipal.getProvider());

    if (strategy == null) {
      throw new IllegalArgumentException("지원하지 않는 플랫폼입니다.");
    }

    strategy.logout(userPrincipal.getId(), res);
    authRedisRepository.deleteRefreshToken(userPrincipal.getId());
  }

  public void refresh(HttpServletRequest req, HttpServletResponse res) {
    Cookie[] cookies = req.getCookies();
    if (cookies == null) {
      throw new AuthException(AuthErrorCode.JWT_TOKEN_NOT_FOUND);
    }

    // 1. 플랫폼별 refresh 쿠키 탐색
    String refreshToken = null;
    OAuthPlatForm detectedProvider = null;

    for (Cookie cookie : cookies) {
      if (cookie.getValue() == null || cookie.getValue().isEmpty()) {
        continue;
      }

      for (OAuthPlatForm platform : OAuthPlatForm.values()) {
        String cookieName = CookieName.REFRESH_TOKEN.of(platform);
        if (!cookie.getName().equals(cookieName)) {
          continue;
        }

        if (detectedProvider != null && detectedProvider != platform) {
          throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshToken = cookie.getValue();
        detectedProvider = platform;
      }
    }

    if (refreshToken == null) {
      throw new AuthException(AuthErrorCode.JWT_TOKEN_NOT_FOUND);
    }

    // 2. userId 파싱
    Long userId = Long.parseLong(jwtService.getSubject(refreshToken));

    // 3. Redis 서명 + 저장값 검증
    if (!jwtService.validateRefreshToken(refreshToken, userId)) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 4. providerId 조회
    String providerId = resolveProviderId(userId, detectedProvider);

    // 5. 새 토큰 발급 (Refresh Token Rotation)
    TokenResponse tokenResponse = jwtService.generateTokenResponse(userId, detectedProvider, providerId);

    // 6. 쿠키 갱신
    cookieManager.addAuthCookies(res, tokenResponse, detectedProvider);

    log.info("[Token Refresh] userId={}, provider={}", userId, detectedProvider);
  }

  public MeResponse getMe(UserPrincipal principal) {
    Long userId = principal.getId();
    OAuthPlatForm platform = principal.getProvider();

    return switch (platform) {
      case LICHESS -> lichessUserRepository.findById(userId)
          .map(u -> new MeResponse(userId, u.getUsername(), OAuthPlatForm.LICHESS,
              imageUtil.getProfileImageUrl(userId, u.getProfile()), u.getDescription()))
          .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
      case CHESSCOM -> chesscomUserRepository.findById(userId)
          .map(u -> new MeResponse(userId, u.getUsername(), OAuthPlatForm.CHESSCOM,
              imageUtil.getProfileImageUrl(userId, u.getProfile()), u.getDescription()))
          .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND_USER));
    };
  }

  private String resolveProviderId(Long userId, OAuthPlatForm provider) {
    return switch (provider) {
      case LICHESS -> lichessUserRepository.findById(userId)
          .map(u -> u.getLichessId())
          .orElseThrow(() -> new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT));
      case CHESSCOM -> chesscomUserRepository.findById(userId)
          .map(u -> String.valueOf(u.getChesscomId()))
          .orElseThrow(() -> new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT));
    };
  }
}
