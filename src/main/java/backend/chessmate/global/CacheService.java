package backend.chessmate.global;

import backend.chessmate.global.external.dto.account.PerfsDto;
import backend.chessmate.global.external.dto.account.PlayTimeDto;
import backend.chessmate.global.external.dto.account.UserCountDto;
import backend.chessmate.global.config.RedisKeyProperties;
import backend.chessmate.global.config.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheService {
  private final RedisService redisService;
  private final RedisKeyProperties redisKeyProperties;

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
  * - Lichess OAuth 토큰 저장: lichessId 값을 키로 사용하여 OAuth 토큰을 Redis에 저장합니다. 유효 기간은 24시간(86400초)입니다.
  * - Lichess OAuth 토큰 조회: lichessId 값을 사용하여 Redis에서
  * - Lichess OAuth 토큰 삭제: lichessId 값을 사용하여 Redis에서 OAuth 토큰을 삭제합니다.
  * */
  public void saveLichessToken(String lichessId, String oauthToken) {
    redisService.save(
        redisKeyProperties.getOauth().lichessToken(lichessId),
        oauthToken,
        86400L
    );
  }

  public String getLichessToken(String lichessId) {
    return redisService.get(
        redisKeyProperties.getOauth().lichessToken(lichessId),
        String.class
    );
  }

  public void deleteLichessToken(String lichessId) {
    redisService.delete(
        redisKeyProperties.getOauth().lichessToken(lichessId)
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

}
