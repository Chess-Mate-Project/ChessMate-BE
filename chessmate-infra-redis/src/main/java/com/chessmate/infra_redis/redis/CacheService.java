package com.chessmate.infra_redis.redis;

import com.chessmate.common.type.GameType;
import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.external.dto.account.PerfsDto;
import com.chessmate.external.dto.account.PlayTimeDto;
import com.chessmate.external.dto.account.UserCountDto;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 도메인별 캐싱 로직을 제공하는 서비스
 *
 * RedisService의 저수준 명령을 래핑하여 고수준 캐싱 기능 제공합니다.
 *
 * 계층 구조:
 * - RedisService: 저수준 Redis 명령 (save, get, delete, brPop 등)
 * - CacheService: 고수준 도메인별 캐싱 로직 (Auth, User, Ranking, Stat 등)
 * - Service: 비즈니스 로직 + Cache-Aside 패턴
 *
 * 캐시 전략:
 * - Auth: 짧은 TTL (5분~7일)
 * - User: 중간 TTL (24시간)
 * - Ranking: 짧은 TTL (1시간) - 실시간성 유지
 * - Generic: 가변 TTL
 */
@Service
@RequiredArgsConstructor
public class CacheService {
  private final RedisService redisService;
  private final RedisKeyProperties redisKeyProperties;
  private final RedisTemplate<String, Object> redisTemplate;

  // ============================================
  // Auth Cache Methods (OauthService)
  // ============================================

  /**
   * 리프레시 토큰 저장
   *
   * TTL: 7일 (604800초)
   * - JWT 리프레시 토큰 만료 주기에 맞춤
   * - 로그인 유지 기간과 동일
   *
   * @param userId 사용자 ID
   * @param refreshToken 저장할 리프레시 토큰
   */
  public void saveRefreshToken(Long userId, String refreshToken) {
    redisService.save(
        redisKeyProperties.getAuth().refreshToken(userId),
        refreshToken,
        604800L
    );
  }

  /**
   * 리프레시 토큰 조회
   *
   * @param userId 사용자 ID
   * @return 저장된 리프레시 토큰 (없으면 null)
   */
  public String getRefreshToken(Long userId) {
    return redisService.get(
        redisKeyProperties.getAuth().refreshToken(userId),
        String.class
    );
  }

  /**
   * 리프레시 토큰 삭제
   *
   * 사용 시점:
   * - 사용자 로그아웃
   * - 토큰 무효화
   *
   * @param userId 사용자 ID
   */
  public void deleteRefreshToken(Long userId) {
    redisService.delete(
        redisKeyProperties.getAuth().refreshToken(userId)
    );
  }

  /**
   * PKCE (OAuth 보안) 저장
   *
   * TTL: 5분 (300초)
   * - OAuth 코드 교환 대기 시간
   * - 보안상 짧은 TTL 유지
   *
   * PKCE란: Proof Key for Public Clients
   * - OAuth2 인증 흐름의 보안 강화
   * - State와 Code Verifier 검증
   *
   * @param state OAuth state 값 (PKCE 검증용)
   * @param codeVerifier 저장할 code verifier
   */
  public void savePkce(String state, String codeVerifier) {
    redisService.save(
        redisKeyProperties.getOauth().pkce(state),
        codeVerifier,
        300
    );
  }

  /**
   * PKCE (OAuth 보안) 조회
   *
   * @param state OAuth state 값
   * @return 저장된 code verifier (없으면 null)
   */
  public String getPkce(String state) {
    return redisService.get(
        redisKeyProperties.getOauth().pkce(state),
        String.class
    );
  }

  /**
   * PKCE (OAuth 보안) 삭제
   *
   * 사용 시점:
   * - OAuth 코드 교환 완료
   * - 인증 프로세스 완료
   *
   * @param state OAuth state 값
   */
  public void deletePkce(String state) {
    redisService.delete(
        redisKeyProperties.getOauth().pkce(state)
    );
  }

  /**
   * Lichess OAuth 토큰 저장
   *
   * TTL: 3일 (259200초)
   * - API 호출 효율성 향상
   * - 매 요청마다 OAuth 서버 호출 불필요
   * - 사용자 세션 유지 기간 연장
   *
   * @param id 사용자 ID
   * @param oauthToken 저장할 Lichess OAuth 토큰
   */
  public void saveLichessToken(Long id, String oauthToken) {
    redisService.save(
        redisKeyProperties.getOauth().lichessToken(id),
        oauthToken,
        259200L
    );
  }

  /**
   * Lichess OAuth 토큰 조회
   *
   * @param id 사용자 ID
   * @return 저장된 OAuth 토큰 (없으면 null)
   */
  public String getLichessToken(Long id) {
    return redisService.get(
        redisKeyProperties.getOauth().lichessToken(id),
        String.class
    );
  }

  /**
   * Lichess OAuth 토큰 삭제
   *
   * 사용 시점:
   * - 사용자 로그아웃
   * - 토큰 갱신
   *
   * @param id 사용자 ID
   */
  public void deleteLichessToken(Long id) {
    redisService.delete(
        redisKeyProperties.getOauth().lichessToken(id)
    );
  }

  // ============================================
  // User Info Cache Methods (LichessApiService)
  // ============================================

  /**
   * 사용자 플레이 시간 저장
   *
   * TTL: 24시간 (86400초)
   * - 사용자 활동 통계 캐싱
   * - Lichess API 호출 최소화
   *
   * @param lichessId Lichess 사용자 ID
   * @param playTime 저장할 플레이 시간 정보
   */
  public void savePlayTime(String lichessId, PlayTimeDto playTime) {
    redisService.save(
        redisKeyProperties.getUser().playtime(lichessId),
        playTime,
        86400L
    );
  }

  /**
   * 사용자 플레이 시간 조회
   *
   * @param lichessId Lichess 사용자 ID
   * @return 저장된 플레이 시간 정보 (없으면 null)
   */
  public PlayTimeDto getPlayTime(String lichessId) {
    return redisService.get(
        redisKeyProperties.getUser().playtime(lichessId),
        PlayTimeDto.class
    );
  }

  /**
   * 사용자 플레이 시간 삭제
   *
   * @param lichessId Lichess 사용자 ID
   */
  public void deletePlayTime(String lichessId) {
    redisService.delete(
        redisKeyProperties.getUser().playtime(lichessId)
    );
  }

  /**
   * 사용자 게임 통계 (Perfs) 저장
   *
   * TTL: 24시간 (86400초)
   * - 게임 타입별 성능 통계
   * - Rapid, Blitz, Bullet, Classical 등의 레이팅
   *
   * @param lichessId Lichess 사용자 ID
   * @param perfs 저장할 게임 통계 정보
   */
  public void savePerfs(String lichessId, PerfsDto perfs) {
    redisService.save(
        redisKeyProperties.getUser().perfs(lichessId),
        perfs,
        86400L
    );
  }

  /**
   * 사용자 게임 통계 (Perfs) 조회
   *
   * @param lichessId Lichess 사용자 ID
   * @return 저장된 게임 통계 정보 (없으면 null)
   */
  public PerfsDto getPerfs(String lichessId) {
    return redisService.get(
        redisKeyProperties.getUser().perfs(lichessId),
        PerfsDto.class
    );
  }

  /**
   * 사용자 게임 통계 (Perfs) 삭제
   *
   * @param lichessId Lichess 사용자 ID
   */
  public void deletePerfs(String lichessId) {
    redisService.delete(
        redisKeyProperties.getUser().perfs(lichessId)
    );
  }

  /**
   * 사용자 게임 플레이 횟수 저장
   *
   * TTL: 24시간 (86400초)
   * - 사용자의 총 게임 플레이 횟수
   * - 게임 타입별 플레이 횟수
   *
   * @param lichessId Lichess 사용자 ID
   * @param userCount 저장할 플레이 횟수 정보
   */
  public void saveUserCount(String lichessId, UserCountDto userCount) {
    redisService.save(
        redisKeyProperties.getUser().playCount(lichessId),
        userCount,
        86400L
    );
  }

  /**
   * 사용자 게임 플레이 횟수 삭제
   *
   * @param lichessId Lichess 사용자 ID
   */
  public void deleteUserCount(String lichessId) {
    redisService.delete(
        redisKeyProperties.getUser().playCount(lichessId)
    );
  }

  // ============================================
  // Ranking Cache Methods (RankService)
  // ============================================

  /**
   * 게임 타입별 전체 랭킹 저장
   *
   * TTL: 1시간 (3600초)
   * - 실시간성 유지: 1시간마다 갱신
   * - DB 부하 감소
   *
   * Cache-Aside 패턴:
   * - 요청 -> 캐시 확인 -> HIT: 반환
   * - 요청 -> 캐시 확인 -> MISS: DB 조회 -> 캐시 저장 -> 반환
   *
   * @param gameType 게임 타입 (RAPID, BLITZ, BULLET, CLASSICAL)
   * @param rankings 저장할 사용자 퍼포먼스 리스트 (레이팅 순 정렬)
   */
  public void saveRanking(GameType gameType, List<UserPerf> rankings) {
    String key = buildRankingCacheKey(gameType);
    redisService.save(key, rankings, 3600);
  }

  /**
   * 게임 타입별 전체 랭킹 조회
   *
   * Cache-Aside 패턴:
   * - 캐시 미스 시 RankService에서 DB 조회
   *
   * @param gameType 게임 타입
   * @return 캐시된 랭킹 리스트 (없으면 null)
   */
  @SuppressWarnings("unchecked")
  public List<UserPerf> getRanking(GameType gameType) {
    String key = buildRankingCacheKey(gameType);
    Object result = redisTemplate.opsForValue().get(key);
    return (List<UserPerf>) result;
  }

  /**
   * 특정 게임 타입의 랭킹 캐시 삭제
   *
   * 사용 시점:
   * - UserPerf 업데이트 완료
   * - 배치 작업 완료
   * - 캐시 무효화 필요 시
   *
   * @param gameType 게임 타입
   */
  public void deleteRanking(GameType gameType) {
    String key = buildRankingCacheKey(gameType);
    redisService.delete(key);
  }

  /**
   * 모든 게임 타입의 랭킹 캐시 삭제
   *
   * 사용 시점:
   * - 전체 배치 작업 완료
   * - 시스템 유지보수
   * - 캐시 완전 갱신 필요 시
   */
  public void deleteAllRankings() {
    for (GameType gameType : GameType.values()) {
      deleteRanking(gameType);
    }
  }

  private String buildRankingCacheKey(GameType gameType) {
    return String.format("rank:ranking:%s", gameType.name());
  }

  // ============================================
  // Generic Cache Methods (Stat Service 등)
  // ============================================

  /**
   * 범용 캐시 저장
   *
   * 용도:
   * - 도메인별 캐싱 메서드가 없을 때 사용
   * - StatService의 통계 데이터 캐싱
   * - 커스텀 캐시 키 필요 시
   *
   * @param key Redis 키 (커스텀 형식 가능)
   * @param value 저장할 객체
   * @param expirationSeconds TTL (초)
   * @param <T> 저장할 객체 타입
   */
  public <T> void saveCache(String key, T value, long expirationSeconds) {
    redisService.save(key, value, expirationSeconds);
  }

  /**
   * 범용 캐시 조회
   *
   * @param key Redis 키
   * @param type 조회할 클래스 타입
   * @return 저장된 객체 (없으면 null)
   * @param <T> 조회할 객체 타입
   */
  public <T> T getCache(String key, Class<T> type) {
    return redisService.get(key, type);
  }

  /**
   * 범용 캐시 삭제
   *
   * @param key Redis 키
   */
  public void deleteCache(String key) {
    redisService.delete(key);
  }

  /**
   * List 타입 캐시 저장 (RatingHistory 등)
   *
   * 용도:
   * - RatingHistory: 사용자의 레이팅 변화 기록
   * - 일반적인 List 데이터 캐싱
   *
   * RedisTemplate 직접 사용:
   * - Jackson 직렬화/역직렬화로 List 타입 지원
   *
   * @param key Redis 키
   * @param value 저장할 List 객체
   * @param expirationSeconds TTL (초)
   * @param <T> List 요소 타입
   */
  public <T> void saveListCache(String key, List<T> value, long expirationSeconds) {
    redisTemplate.opsForValue().set(key, value, expirationSeconds, TimeUnit.SECONDS);
  }

  /**
   * List 타입 캐시 조회 (RatingHistory 등)
   *
   * @param key Redis 키
   * @return 저장된 List 객체 (없으면 null)
   * @param <T> List 요소 타입
   */
  @SuppressWarnings("unchecked")
  public <T> List<T> getListCache(String key) {
    return (List<T>) redisTemplate.opsForValue().get(key);
  }
}

