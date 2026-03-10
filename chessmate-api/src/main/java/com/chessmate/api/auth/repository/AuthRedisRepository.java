package com.chessmate.api.auth.repository;

import com.chessmate.api.auth.AuthRedisKetPrefix;
import com.chessmate.api.auth.AuthRedisPrefix;
import com.chessmate.api.auth.OAuthRedisPrefix;
import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * OAuth 플랫폼별 토큰을 Redis에서 관리하는 Repository.
 * <p>
 * Chess.com과 Lichess의 OAuth 토큰을 저장, 조회, 삭제하는 기능을 제공합니다.
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class AuthRedisRepository {

  private final RedisService redisService;

  // ==================== Chess.com OAuth Token Management ====================

  /**
   * 사용자의 Chess.com OAuth 토큰을 Redis에서 삭제합니다.
   *
   * @param id 사용자 ID
   */
  public void deleteChesscomToken(Long id) {
    String key = OAuthRedisPrefix.CHESSCOM_OAUTH_TOKEN.bind(id);
    redisService.delete(key);
  }

  /**
   * 사용자의 Chess.com OAuth 토큰을 Redis에서 조회합니다.
   *
   * @param id 사용자 ID
   * @return Chess.com OAuth 토큰, 없으면 null
   */
  public String getChesscomToken(Long id) {
    String key = OAuthRedisPrefix.CHESSCOM_OAUTH_TOKEN.bind(id);
    return redisService.get(key, String.class);
  }

  /**
   * 사용자의 Chess.com OAuth 토큰을 Redis에 저장합니다.
   *
   * @param id 사용자 ID
   * @param token Chess.com OAuth 토큰
   * @param ttlSeconds 토큰 유효 기간 (초 단위)
   */
  public void saveChesscomToken(Long id, String token, long ttlSeconds) {
    String key = OAuthRedisPrefix.CHESSCOM_OAUTH_TOKEN.bind(id);
    redisService.save(key, token, ttlSeconds);
  }

  // ==================== Lichess OAuth Token Management ====================

  /**
   * 사용자의 Lichess OAuth 토큰을 Redis에서 조회합니다.
   *
   * @param id 사용자 ID
   * @return Lichess OAuth 토큰, 없으면 null
   */
  public String getLichessToken(Long id) {
    String key = OAuthRedisPrefix.LICHESS_OAUTH_TOKEN.bind(id);
    return redisService.get(key, String.class);
  }

  /**
   * 사용자의 Lichess OAuth 토큰을 Redis에 저장합니다.
   *
   * @param id 사용자 ID
   * @param token Lichess OAuth 토큰
   * @param ttlSeconds 토큰 유효 기간 (초 단위)
   */
  public void saveLichessToken(Long id, String token, long ttlSeconds) {
    String key = OAuthRedisPrefix.LICHESS_OAUTH_TOKEN.bind(id);
    redisService.save(key, token, ttlSeconds);
  }

  /**
   * 사용자의 Lichess OAuth 토큰을 Redis에서 삭제합니다.
   *
   * @param id 사용자 ID
   */
  public void deleteLichessToken(Long id) {
    String key = OAuthRedisPrefix.LICHESS_OAUTH_TOKEN.bind(id);
    redisService.delete(key);
  }

  // ==================== Refresh Token Management ====================

  /**
   * 사용자의 Refresh 토큰을 Redis에서 조회합니다.
   *
   * @param id 사용자 ID
   * @return Refresh 토큰, 없으면 null
   */
  public String getRefreshToken(Long id) {
    String key = AuthRedisPrefix.REFRESH_TOKEN.bind(id);
    return redisService.get(key, String.class);
  }

  public void saveRefreshToken(Long id, String token, long ttlSeconds) {
    String key = AuthRedisPrefix.REFRESH_TOKEN.bind(id);
    redisService.save(key, token, ttlSeconds);
  }

  public void deleteRefreshToken(Long id) {
    String key = AuthRedisPrefix.REFRESH_TOKEN.bind(id);
    redisService.delete(key);
  }

}
