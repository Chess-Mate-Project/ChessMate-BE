package com.chessmate.api.auth.service;

import com.chessmate.api.auth.jwt.JwtRule;
import com.chessmate.api.auth.jwt.JwtService;
import com.chessmate.api.auth.repository.AuthRedisRepository;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.infra_core.entity.OauthPlatForm;
import com.chessmate.infra_core.entity.Profile;
import com.chessmate.infra_core.repository.ProfileRepository;
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
  private final ProfileRepository profileRepository;

  public void logout(Long id, HttpServletResponse res) throws Throwable {
    jwtService.logout(res); //쿠키 만료

    Profile profile = profileRepository.findById(id)
        .orElseThrow(() -> new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT));

    switch (profile.getProvider()) {
      case OauthPlatForm.CHESSCOM -> authRedisRepository.deleteChesscomToken(id);
      case OauthPlatForm.LICHESS -> authRedisRepository.deleteLichessToken(id);
    }
  }

  public void refresh(HttpServletRequest req, HttpServletResponse res) {
    String refreshToken = jwtService.resolveToken(req, JwtRule.REFRESH_PREFIX);
    Long id = Long.valueOf(jwtService.getSubject(refreshToken));

    if (refreshToken == null || !jwtService.validateRefreshToken(refreshToken, id)) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    Profile profile = profileRepository.findById(id)
        .orElseThrow(() -> new AuthException(AuthErrorCode.FAILD_GET_USER_ACCOUNT));

    if (!authRedisRepository.getRefreshToken(id).equals(refreshToken)) {
      throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    jwtService.generateAccessToken(res, profile);
  }
}
