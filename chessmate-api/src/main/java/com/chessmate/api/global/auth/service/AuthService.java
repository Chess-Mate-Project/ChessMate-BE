package com.chessmate.api.global.auth.service;

import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.global.auth.jwt.JwtService;
import com.chessmate.api.global.auth.service.strategy.LogoutStrategy;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.chesscom.user.repositoryImpl.ChesscomUserRepositoryImpl;
import com.chessmate.infra_redis.repository.AuthRedisRepository;
import com.chessmate.infra_persistence.lichess.user.repositoryImpl.LichessUserRepositoryImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthService {

  private final JwtService jwtService;
  private final AuthRedisRepository authRedisRepository;
  private final Map<OAuthPlatForm, LogoutStrategy> logoutStrategyMap;

  // 생성자 주입 시점에 모든 전략을 Map으로 변환하여 저장
  public AuthService(JwtService jwtService, AuthRedisRepository authRedisRepository, List<LogoutStrategy> strategies) {
    this.jwtService = jwtService;
    this.authRedisRepository = authRedisRepository;
    this.logoutStrategyMap = strategies.stream()
        .collect(Collectors.toMap(LogoutStrategy::getProvider, s -> s));
  }

  public void logout(UserPrincipal userPrincipal, HttpServletResponse res) {
    LogoutStrategy strategy = logoutStrategyMap.get(userPrincipal.getProvider());

    if (strategy == null) {
      throw new IllegalArgumentException("지원하지 않는 플랫폼입니다.");
    }

    strategy.logout(userPrincipal.getId(), res);
  }



/*
  public void logout(User user, HttpServletResponse res) {
    jwtService.logout(res); //쿠키 만료
    cacheService.deleteRefreshToken(user.getId());

    cacheService.deleteLichessToken(user.getId());
  }

  public void refresh(HttpServletRequest req, HttpServletResponse res) {
    String refreshToken = jwtService.resolveToken(req, JwtRule.REFRESH_PREFIX);
    String userId = jwtService.getSubject(refreshToken);
    log.info("[토큰 재발급 요청] userId={}, refreshToken={}", userId, refreshToken);

    if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken, Long.valueOf(userId))) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    User user = userRepository.findById(Long.valueOf(userId))
        .orElseThrow(() -> new RuntimeException("유저 없음"));


    if (!cacheService.getRefreshToken(user.getId()).equals(refreshToken)) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    // 마지막 접속 시간 갱신
    user.setLastLoginAt(java.time.LocalDateTime.now());
    userRepository.save(user);

    jwtService.generateAccessToken(res, user);
  }*/
}
