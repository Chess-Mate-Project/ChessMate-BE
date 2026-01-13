package com.chessmate.api.auth.service;

import com.chessmate.api.auth.jwt.JwtRule;
import com.chessmate.api.auth.jwt.JwtService;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.entity.UserEntity;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.infra_redis.redis.CacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final JwtService jwtService;
  private final CacheService cacheService;
  private final UserRepositoryImpl userRepository;

  public void logout(User user, HttpServletResponse res) {
    jwtService.logout(res); //쿠키 만료
    cacheService.deleteRefreshToken(user.getId());

    cacheService.deleteLichessToken(user.getLichessId());
  }

  public void refresh(HttpServletRequest req, HttpServletResponse res) {
    String refreshToken = jwtService.resolveToken(req, JwtRule.REFRESH_PREFIX);
    String userId = jwtService.getSubject(refreshToken);

    if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken, Long.valueOf(userId))) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    User user = userRepository.findById(Long.valueOf(userId))
        .orElseThrow(() -> new RuntimeException("유저 없음"));


    if (!cacheService.getRefreshToken(user.getId()).equals(refreshToken)) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    jwtService.generateAccessToken(res, user);
  }
}
