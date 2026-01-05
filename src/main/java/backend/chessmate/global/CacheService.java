package backend.chessmate.global;

import backend.chessmate.api.external.dto.account.PerfsDto;
import backend.chessmate.api.external.dto.account.PlayTimeDto;
import backend.chessmate.api.external.dto.account.UserCountDto;
import backend.chessmate.global.config.RedisKeyProperties;
import backend.chessmate.global.config.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheService {
  private final RedisService redisService;
  private final RedisKeyProperties redisKeyProperties;

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
