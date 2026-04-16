package com.chessmate.api.global.auth.jwt;

import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.global.auth.dto.TokenResponse;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_redis.repository.AuthRedisRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

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


  public String generateAccessToken(
      Long id,
      OAuthPlatForm provider,
      String providerId
  ) {
    return generator.generateAccessToken(ACCESS_KEY, ACCESS_EXP, id, provider, providerId);
  }

  public String generateRefreshToken(Long id, OAuthPlatForm provider) {
    String rt = generator.generateRefreshToken(REFRESH_KEY, REFRESH_EXP, id, provider);
    authRedisRepository.saveRefreshToken(id, rt, (int) (REFRESH_EXP / 1000));
    return rt;
  }

  public TokenResponse generateTokenResponse(Long id, OAuthPlatForm provider, String providerId) {
    String accessToken = this.generateAccessToken(id, provider, providerId);
    String refreshToken = this.generateRefreshToken(id, provider);
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
    OAuthPlatForm provider = OAuthPlatForm.valueOf(claims.get("provider", String.class));

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

  public String getSubject(String refreshToken) {
    return Jwts.parserBuilder()
        .setSigningKey(REFRESH_KEY)
        .build()
        .parseClaimsJws(refreshToken)
        .getBody()
        .getSubject();
  }

  public OAuthPlatForm getProviderFromRefreshToken(String refreshToken) {
    String provider = Jwts.parserBuilder()
        .setSigningKey(REFRESH_KEY)
        .build()
        .parseClaimsJws(refreshToken)
        .getBody()
        .get("provider", String.class);
    return OAuthPlatForm.valueOf(provider);
  }

}
