package com.chessmate.api.auth.jwt;

import static com.chessmate.api.auth.jwt.JwtRule.ACCESS_PREFIX;
import static com.chessmate.api.auth.jwt.JwtRule.JWT_ISSUE_HEADER;
import static com.chessmate.api.auth.jwt.JwtRule.REFRESH_PREFIX;

import com.chessmate.common.code.AuthErrorCode;
import com.chessmate.common.exception.AuthException;
import com.chessmate.infra_redis.redis.RedisService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Key;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtService {
  private final JwtGenerator generator;
  private final JwtUtil util;
  private final RedisService redisService;
  private final Key accessKey;
  private final Key refreshKey;
  private final long accessExp;
  private final long refreshExp;
  private final String refreshTokenKey;
  private final String cookieDomain;

  public JwtService(
      JwtGenerator generator,
      JwtUtil util,
      RedisService redisService,
      @Value("${spring.jwt.access-token.secret}") String accessSecret,
      @Value("${spring.jwt.refresh-token.secret}") String refreshSecret,
      @Value("${spring.jwt.access-token.expiration}") long accessExpiration,
      @Value("${spring.jwt.refresh-token.expiration}") long refreshExpiration,
      @Value("${spring.data.redis.key.refresh_token_base}") String refreshTokenKey,
      @Value("${cookie.domain}") String cookieDomain) {
    this.generator = generator;
    this.util = util;
    this.redisService = redisService;
    this.accessKey = util.getSigningKey(accessSecret);
    this.refreshKey = util.getSigningKey(refreshSecret);
    this.accessExp = accessExpiration;
    this.refreshExp = refreshExpiration;
    this.refreshTokenKey = refreshTokenKey;
    this.cookieDomain = cookieDomain;
  }

  /**
   * Access Token 발급 후 응답 쿠키에 추가
   */
  @Transactional
  @SuppressWarnings("UnusedReturnValue")
  public String generateAccessToken(HttpServletResponse res, Long id) {
    String accessToken = generator.generateAccessToken(accessKey, accessExp, String.valueOf(id));

    ResponseCookie cookie = ResponseCookie.from(ACCESS_PREFIX.getValue(), accessToken)
        .path("/")
        .domain(cookieDomain)
        .httpOnly(false)
        .secure(true)
        .sameSite("Lax")
        .maxAge(60)
        .build();
    res.addHeader(JWT_ISSUE_HEADER.getValue(), cookie.toString());

    return accessToken;
  }

  /**
   * Refresh Token 발급 + Redis 저장 (RTR 전략)
   */
  @Transactional
  @SuppressWarnings("UnusedReturnValue")
  public String generateRefreshToken(HttpServletResponse res, Long id) {
    String refreshToken = generator.generateRefreshToken(refreshKey, refreshExp, String.valueOf(id));

    ResponseCookie cookie = ResponseCookie.from(REFRESH_PREFIX.getValue(), refreshToken)
        .path("/")
        .domain(cookieDomain)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .maxAge(refreshExp / 1000)
        .build();
    res.addHeader(JWT_ISSUE_HEADER.getValue(), cookie.toString());

    redisService.save(refreshTokenKey + id, refreshToken, refreshExp);
    return refreshToken;
  }

  /**
   * Access Token 유효성 검증
   */
  public boolean validateAccessToken(String token) {
    return util.getTokenStatus(token, accessKey) == TokenStatus.AUTHENTICATED;
  }

  /**
   * Refresh Token 유효성 검증 (서명 + Redis 일치도 확인)
   */
  public boolean validateRefreshToken(String token, Long identifier) {
    if (util.getTokenStatus(token, refreshKey) != TokenStatus.AUTHENTICATED) {
      return false;
    }

    String key = refreshTokenKey + identifier;
    String stored = redisService.get(key, String.class);
    return token.equals(stored);
  }

  /**
   * JWT로부터 Authentication 객체 생성
   */
  @SuppressWarnings("unused")
  public Authentication getAuthentication(String token) {
    // TODO: Authentication 객체 반환 구현 필요
    return null;
  }

  /**
   * 요청의 쿠키에서 JWT 추출
   */
  public String resolveToken(HttpServletRequest req, JwtRule rule) {
    jakarta.servlet.http.Cookie[] cookies = req.getCookies();
    if (cookies == null) {
      throw new AuthException(AuthErrorCode.JWT_TOKEN_NOT_FOUND);
    }
    return util.resolveTokenFromCookie(cookies, rule);
  }

  /**
   * 로그아웃 처리 (쿠키 만료)
   */
  @Transactional
  public void logout(HttpServletResponse res) {
    res.addCookie(util.resetToken(ACCESS_PREFIX));
    res.addCookie(util.resetToken(REFRESH_PREFIX));
  }

  /**
   * Refresh Token에서 Subject 추출
   */
  public String getSubject(String refreshToken) {
    return Jwts.parserBuilder()
        .setSigningKey(refreshKey)
        .build()
        .parseClaimsJws(refreshToken)
        .getBody()
        .getSubject();
  }

}
