package backend.chessmate.api.auth.service;

import backend.chessmate.api.auth.jwt.JwtRule;
import backend.chessmate.api.auth.jwt.JwtService;
import backend.chessmate.api.user.entity.User;
import backend.chessmate.api.oauth.repository.UserRepository;
import backend.chessmate.global.CacheService;
import backend.chessmate.global.common.code.AuthErrorCode;
import backend.chessmate.global.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final JwtService jwtService;
  private final CacheService cacheService;
  private final UserRepository userRepository;

  public void logout(User user, HttpServletResponse res) {
    jwtService.logout(res); //쿠키 만료
    cacheService.deleteRefreshToken(user.getId());
    cacheService.deleteLichessToken(user.getLichessId());
  }

  public void refresh(HttpServletRequest req, HttpServletResponse res) {
    String refreshToken = jwtService.resolveToken(req, JwtRule.REFRESH_PREFIX);
    String userId = jwtService.getSubject(refreshToken);
    User user = userRepository.findById(Long.valueOf(userId))
        .orElseThrow(() -> new RuntimeException("유저 없음"));

    if (!jwtService.validateRefreshToken(refreshToken, user.getId())) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    jwtService.generateAccessToken(res, user);
    jwtService.generateRefreshToken(res, user);
  }
}
