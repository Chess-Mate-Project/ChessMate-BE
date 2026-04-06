package com.chessmate.api.global.auth.jwt;


import static com.chessmate.api.global.auth.jwt.JwtRule.ACCESS_PREFIX;
import static com.chessmate.api.global.auth.jwt.JwtRule.REFRESH_PREFIX;

import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.exception.AuthException;
import com.chessmate.infra_redis.prefix.AuthRedisPrefix;
import com.chessmate.infra_redis.redis.RedisService;
import com.chessmate.infra_redis.repository.AuthRedisRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Key;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JwtService {
  private final JwtGenerator generator;
  private final JwtUtil util;
  private final AuthRedisRepository authRedisRepository;

  private final Key ACCESS_KEY;
  private final Key REFRESH_KEY;
  private final long ACCESS_EXP;
  private final long REFRESH_EXP;

  public JwtService(
      JwtGenerator jwtGenerator,
      JwtUtil jwtUtil, AuthRedisRepository authRedisRepository,
      @Value("${spring.jwt.access-token.secret}") String accessSecret,
      @Value("${spring.jwt.refresh-token.secret}") String refreshSecret,
      @Value("${spring.jwt.access-token.expiration}") long accessExpiration,
      @Value("${spring.jwt.refresh-token.expiration}") long refreshExpiration
  ) {
    this.generator = jwtGenerator;
    this.util = jwtUtil;
    this.authRedisRepository = authRedisRepository;
    this.ACCESS_KEY = jwtUtil.getSigningKey(accessSecret);
    this.REFRESH_KEY = jwtUtil.getSigningKey(refreshSecret);
    this.ACCESS_EXP = accessExpiration;
    this.REFRESH_EXP = refreshExpiration;
  }


  @Transactional
  public String generateAccessToken(
      Long id,
      OAuthPlatForm provider,
      String providerId
  ) {
    return generator.generateAccessToken(ACCESS_KEY, ACCESS_EXP, id, provider, providerId);
  }

  @Transactional
  public String generateRefreshToken(Long id) {
    String rt = generator.generateRefreshToken(REFRESH_KEY, REFRESH_EXP, id);
    authRedisRepository.saveRefreshToken(id, rt, (int) (REFRESH_EXP / 1000));
    return rt;
  }

  @Transactional
  public TokenResponse generateTokenResponse(Long id, OAuthPlatForm provider, String providerId) {
    String accessToken = this.generateAccessToken(id, provider, providerId);
    String refreshToken = this.generateRefreshToken(id);
    return new TokenResponse(accessToken, ACCESS_EXP, refreshToken, REFRESH_EXP, "Bearer");
  }

  // 3) Access Token 검증
  public boolean validateAccessToken(String t) {
    TokenStatus status = util.getTokenStatus(t, ACCESS_KEY);
    if (status != TokenStatus.AUTHENTICATED) {
      log.warn("JWT 토큰 검증 실패 - 상태: {}, ACCESS_EXP: {}ms", status, ACCESS_EXP);
      return false;
    }
    return true;
  }

  public Authentication getAuthentication(String t) {
    Claims claims = Jwts.parserBuilder()
        .setSigningKey(ACCESS_KEY)
        .build()
        .parseClaimsJws(t)
        .getBody();

    Long id = Long.valueOf(claims.getSubject());
    OAuthPlatForm provider = claims.get("provider", OAuthPlatForm.class);

    return new UsernamePasswordAuthenticationToken(
        new UserPrincipal(id, provider),
        null,
        List.of(new SimpleGrantedAuthority("ROLE_USER"))
    );

  }

  // 4) Refresh Token 검증 (서명 + Redis 일치 여부)
  public boolean validateRefreshToken(String t, Long identifier) {
    boolean ok = util.getTokenStatus(t, REFRESH_KEY) == TokenStatus.AUTHENTICATED;
    if (!ok) return false;

    String stored = authRedisRepository.getRefreshToken(identifier);
    return t.equals(stored);
  }

  public String resolveToken(HttpServletRequest req, JwtRule p) {
    Cookie[] cs = req.getCookies();
    if (cs == null)
      throw new AuthException(AuthErrorCode.JWT_TOKEN_NOT_FOUND);

    return util.resolveTokenFromCookie(cs, p);
  }

  // 7) 로그아웃 처리: 쿠키 만료
  @Transactional
  public void logout(HttpServletResponse res) {
    res.addCookie(util.resetToken(ACCESS_PREFIX));
    res.addCookie(util.resetToken(REFRESH_PREFIX));
  }

  public String getSubject(String refreshToken) {
    return Jwts.parserBuilder()
        .setSigningKey(REFRESH_KEY)
        .build()
        .parseClaimsJws(refreshToken)
        .getBody()
        .getSubject();
  }

}
