package com.chessmate.api.auth.service;

import com.chessmate.api.auth.jwt.JwtRule;
import com.chessmate.api.auth.jwt.JwtService;
import com.chessmate.api.auth.repository.AuthRedisRepository;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.infra_core.entity.User;
import com.chessmate.infra_core.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final JwtService jwtService;
  private final AuthRedisRepository authRedisRepository;
  private final UserRepository userRepository;

  /**
   * 사용자 로그아웃 처리
   * - JWT 쿠키 만료
   * - Redis에 저장된 토큰 삭제
   *
   * @param id 사용자 ID
   * @param res HTTP 응답
   */
  public void logout(Long id, HttpServletResponse res) {
    jwtService.logout(res); //쿠키 만료

    User user = userRepository.findById(id)
        .orElseThrow(() -> new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT));

    user.getProfiles().forEach(profile -> {
      switch (profile.getPlatform()) {
        case CHESSCOM -> authRedisRepository.deleteChesscomToken(id);
        case LICHESS -> authRedisRepository.deleteLichessToken(id);
      }
    });
  }

  /**
   * 리프레시 토큰을 이용한 새 액세스 토큰 발급
   *
   * @param req HTTP 요청
   * @param res HTTP 응답
   */
  public void refresh(HttpServletRequest req, HttpServletResponse res) {
    String refreshToken = jwtService.resolveToken(req, JwtRule.REFRESH_PREFIX);
    Long id = Long.valueOf(jwtService.getSubject(refreshToken));

    if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken, id)) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    User user = userRepository.findById(id)
        .orElseThrow(() -> new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT));

    if (!authRedisRepository.getRefreshToken(id).equals(refreshToken)) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }


    jwtService.generateAccessToken(res, user.getId());
  }
}

