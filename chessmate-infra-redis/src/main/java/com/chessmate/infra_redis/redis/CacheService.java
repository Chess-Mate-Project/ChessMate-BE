package com.chessmate.infra_redis.redis;


import com.chessmate.external.dto.account.PerfsDto;
import com.chessmate.external.dto.account.PlayTimeDto;
import com.chessmate.external.dto.account.UserCountDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheService {
  private final RedisService redisService;
  private final RedisKeyProperties redisKeyProperties;
  private final RedisTemplate<String, Object> redisTemplate;

  public String getRefreshToken(Long userId) {
    return redisService.get(
        redisKeyProperties.getAuth().refreshToken(userId),
        String.class
    );
  }
  /**
  * - PKCE 저장: state 값을 키로 사용하여 code verifier를 Redis에 저장합니다. 유효 기간은 5분(300초)입니다.
  * - PKCE 조회: state 값을 사용하여 Redis에서 code verifier를 조회합니다.
  * - PKCE 삭제: state 값을 사용하여 Redis에서 code verifier를 삭제합니다.
  * */
  public void savePkce(String state, String codeVerifier) {
    redisService.save(
        redisKeyProperties.getOauth().pkce(state),
        codeVerifier,
        300
    );
  }

  public String getPkce(String state) {
    return redisService.get(
        redisKeyProperties.getOauth().pkce(state),
        String.class
    );
  }

  public void deletePkce(String state) {
    redisService.delete(
        redisKeyProperties.getOauth().pkce(state)
    );
  }


  /**
  * - Lichess OAuth 토큰 저장: id 값을 키로 사용하여 OAuth 토큰을 Redis에 저장합니다. 유효 기간은 24시간(86400초)입니다.
  * - Lichess OAuth 토큰 조회: id 값을 사용하여 Redis에서
  * - Lichess OAuth 토큰 삭제: id 값을 사용하여 Redis에서 OAuth 토큰을 삭제합니다.
  * */
  public void saveLichessToken(Long id, String oauthToken) {
    redisService.save(
        redisKeyProperties.getOauth().lichessToken(id),
        oauthToken,
        86400L
    );
  }

  public String getLichessToken(Long id) {
    return redisService.get(
        redisKeyProperties.getOauth().lichessToken(id),
        String.class
    );
  }

  public void deleteLichessToken(Long id) {
    redisService.delete(
        redisKeyProperties.getOauth().lichessToken(id)
    );
  }

  /**
  * - 리프레시 토큰 저장: userId 값을 키로 사용하여 리프레시 토큰을 Redis에 저장합니다. 유효 기간은 7일(604800초)입니다.
  * - 리프레시 토큰 삭제: userId 값을 사용하여 Redis에서 리프레시 토큰을 삭제합니다.
  * -
  * */

  public void saveRefreshToken(Long userId, String refreshToken) {
    redisService.save(
        redisKeyProperties.getAuth().refreshToken(userId),
        refreshToken,
        604800L
    );
  }

  public void deleteRefreshToken(Long userId) {
    redisService.delete(
        redisKeyProperties.getAuth().refreshToken(userId)
    );
  }

  /**
  * - 사용자 플레이 시간 저장: lichessId 값을 키로 사용하여 PlayTimeDto 객체를 Redis에 저장합니다. 유효 기간은 24시간(86400초)입니다.
  * - 사용자 플레이 시간 조회: lichessId 값을 사용하여 Redis에서 PlayTimeDto
  * - 사용자 플레이 시간 삭제: lichessId 값을 사용하여 Redis에서 PlayTimeDto 객체를 삭제합니다.
  * */
  public void savePlayTime(String lichessId, PlayTimeDto playTime) {
    redisService.save(
        redisKeyProperties.getUser().playtime(lichessId),
        playTime,
        86400L
    );
  }

  public PlayTimeDto getPlayTime(String lichessId) {
    return redisService.get(
        redisKeyProperties.getUser().playtime(lichessId),
        PlayTimeDto.class
    );
  }


  public void deletePlayTime(String lichessId) {
    redisService.delete(
        redisKeyProperties.getUser().playtime(lichessId)
    );
  }


  /** - 사용자 퍼포먼스 저장: lichessId 값을 키로 사용하여 PerfsDto 객체를 Redis에 저장합니다. 유효 기간은 24시간(86400초)
  * - 사용자 퍼포먼스 삭제: lichessId 값을 사용하여 Redis에서 PerfsDto 객체를 삭제합니다.
  * -
   */
  public void savePerfs(String lichessId, PerfsDto perfs) {
    redisService.save(
        redisKeyProperties.getUser().perfs(lichessId),
        perfs,
        86400L
    );
  }

  public PerfsDto getPerfs(String lichessId) {
    return redisService.get(
        redisKeyProperties.getUser().perfs(lichessId),
        PerfsDto.class
    );
  }

  public void deletePerfs(String lichessId) {
    redisService.delete(
        redisKeyProperties.getUser().perfs(lichessId)
    );
  }

  public void saveUserCount(String lichessId, UserCountDto userCount) {
    redisService.save(
        redisKeyProperties.getUser().playCount(lichessId),
        userCount,
        86400L
    );
  }

  public void deleteUserCount(String lichessId) {
    redisService.delete(
        redisKeyProperties.getUser().playCount(lichessId)
    );
  }

  /**
   * ====================================
   * Generic Cache Methods (Stat Service)
   * ====================================
   */

  /**
   * 제너릭 캐시 저장
   * @param key Redis 키
   * @param value 저장할 객체
   * @param expirationSeconds TTL (초)
   */
  public <T> void saveCache(String key, T value, long expirationSeconds) {
    redisService.save(key, value, expirationSeconds);
  }

  /**
   * 제너릭 캐시 조회
   * @param key Redis 키
   * @param type 조회할 클래스 타입
   * @return 저장된 객체 (없으면 null)
   */
  public <T> T getCache(String key, Class<T> type) {
    return redisService.get(key, type);
  }

  /**
   * 캐시 삭제
   * @param key Redis 키
   */
  public void deleteCache(String key) {
    redisService.delete(key);
  }

  /**
   * List 타입 캐시 저장 (RatingHistory 등)
   * @param key Redis 키
   * @param value 저장할 List 객체
   * @param expirationSeconds TTL (초)
   */
  public <T> void saveListCache(String key, List<T> value, long expirationSeconds) {
    redisTemplate.opsForValue().set(key, value, expirationSeconds, java.util.concurrent.TimeUnit.SECONDS);
  }

  /**
   * List 타입 캐시 조회 (RatingHistory 등)
   * @param key Redis 키
   * @return 저장된 List 객체 (없으면 null)
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getListCache(String key) {
    return (List<T>) redisTemplate.opsForValue().get(key);
  }

}
