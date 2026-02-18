package com.chessmate.api.stat.service;

import com.chessmate.api.redis.LichessApiProducer;
import com.chessmate.api.stat.dto.ColorStatsResponse;
import com.chessmate.api.stat.dto.DailyStreakDto;
import com.chessmate.api.stat.dto.FirstMoveResponse;
import com.chessmate.api.stat.dto.RatingHistoryDto;
import com.chessmate.api.stat.dto.TierResponse;
import com.chessmate.api.stat.dto.UserPerfResponse;
import com.chessmate.api.stat.dto.YearStreakDto;
import com.chessmate.common.dto.TierResult;
import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.user.User;
import com.chessmate.domain.userColorStat.UserColorStat;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.external.dto.account.PerfsDto;
import com.chessmate.infra_persistence.repositoryImpl.UserColorStatRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserDailyStreakRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserFirstMoveStatRepositoryImpl;
import com.chessmate.infra_redis.redis.CacheService;
import com.chessmate.infra_redis.redis.dto.TaskType;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatService {

  private final UserDailyStreakRepositoryImpl userDailyStreakRepository;
  private final UserColorStatRepositoryImpl userColorStatRepository;
  private final UserFirstMoveStatRepositoryImpl userFirstMoveStatRepository;
  private final UserPerfRepository userPerfRepository;
  private final LichessApiProducer lichessApiProducer;
  private final CacheService cacheService;

  public void forceUpdateUserData(User user) {
    lichessApiProducer.sendSyncTask(user, user.getUsername(), cacheService.getLichessToken(user.getId()), TaskType.FORCE_UPDATE, false);
  }

  @Transactional(readOnly = true)
  public YearStreakDto getDailyStreaksByYear(User user, Year year) {
    // Cache-Aside Pattern 1단계: 캐시에서 조회
    String cacheKey = buildYearStreakCacheKey(user.getId(), year);
    YearStreakDto cachedData = cacheService.getCache(cacheKey, YearStreakDto.class);

    if (cachedData != null) {
      log.info("[Cache-Hit] YearStreak - userId={}, year={}", user.getId(), year.getValue());
      return cachedData;
    }

    log.info("[Cache-Miss] YearStreak - userId={}, year={}, DB 조회 시작", user.getId(), year.getValue());

    // Cache-Aside Pattern 2단계: 캐시 미스 시 DB에서 조회
    List<DailyStreakDto> dailyStreakDto = new ArrayList<>();

    LocalDate start = LocalDate.of(year.getValue(), 1, 1);
    LocalDate end = LocalDate.of(year.getValue(), 12, 31);

    log.info("[YearStreak] 조회 범위 - userId={}, start={}, end={}", user.getId(), start, end);

    var streaks = userDailyStreakRepository.findByUserIdAndYearRange(user.getId(), start, end);

    log.info("[YearStreak] DB 조회 결과 - userId={}, year={}, 데이터 개수={}",
        user.getId(), year.getValue(), streaks.size());

    if (streaks.isEmpty()) {
      log.warn("[YearStreak] [EMPTY] 조회된 데이터가 없음 - userId={}, year={}, start={}, end={}",
          user.getId(), year.getValue(), start, end);
    }

    streaks.forEach(streak -> {
          log.debug("[YearStreak] 변환 전 - date={}, win={}, lose={}, draw={}, lastRating={}",
              streak.getDate(), streak.getWin(), streak.getLose(), streak.getDraw(), streak.getLastRating());

          DailyStreakDto dto = new DailyStreakDto(
              streak.getDate(),
              streak.getWin(),
              streak.getLose(),
              streak.getDraw(),
              (streak.getWin() + streak.getLose() + streak.getDraw()),
              streak.getLastRating()
          );
          dailyStreakDto.add(dto);
          log.debug("[YearStreak] 변환 후 - date={}, win={}, lose={}, draw={}, total={}, lastRating={}",
              dto.date(), dto.win(), dto.lose(), dto.draw(), dto.total(), dto.lastRating());
        });

    YearStreakDto result = new YearStreakDto(year, dailyStreakDto);

    // Cache-Aside Pattern 3단계: 조회 결과를 캐시에 저장 (TTL: 1시간)
    cacheService.saveCache(cacheKey, result, 3600);
    log.info("[Cache-Set] YearStreak - userId={}, year={}, count={}, TTL=3600s",
        user.getId(), year.getValue(), dailyStreakDto.size());

    return result;
  }

  /**
   * 특정 년도의 레이팅 히스토리 조회 (Cache-Aside Pattern 적용)
   * @param user 사용자
   * @param year 조회할 년도
   * @return List<RatingHistoryDto> 일별 lastRating 목록
   */
  @Transactional(readOnly = true)
  public List<RatingHistoryDto> getRatingHistory(User user, Year year) {
    // Cache-Aside Pattern 1단계: 캐시에서 조회
    String cacheKey = buildRatingHistoryCacheKey(user.getId(), year);
    List<RatingHistoryDto> cachedData = cacheService.getListCache(cacheKey);

    if (cachedData != null && !cachedData.isEmpty()) {
      log.info("[Cache-Hit] RatingHistory - userId={}, year={}", user.getId(), year.getValue());
      return cachedData;
    }

    log.info("[Cache-Miss] RatingHistory - userId={}, year={}, DB 조회 시작", user.getId(), year.getValue());

    // Cache-Aside Pattern 2단계: 캐시 미스 시 DB에서 조회
    List<RatingHistoryDto> ratingHistory = new ArrayList<>();

    LocalDate start = LocalDate.of(year.getValue(), 1, 1);
    LocalDate end = LocalDate.of(year.getValue(), 12, 31);

    userDailyStreakRepository.findByUserIdAndYearRange(user.getId(), start, end)
        .forEach(streak -> {
          RatingHistoryDto dto = new RatingHistoryDto(
              streak.getDate(),
              streak.getLastRating()
          );
          ratingHistory.add(dto);
        });

    // Cache-Aside Pattern 3단계: 조회 결과를 캐시에 저장 (TTL: 1시간)
    cacheService.saveListCache(cacheKey, ratingHistory, 3600);
    log.info("[Cache-Set] RatingHistory - userId={}, year={}, count={}, TTL=3600s",
        user.getId(), year.getValue(), ratingHistory.size());

    return ratingHistory;
  }

  @Transactional(readOnly = true)
  public ColorStatsResponse getColorStats(User user, GameType gameType) {
    // Cache-Aside Pattern 1단계: 캐시에서 조회
    String cacheKey = buildColorStatsCacheKey(user.getId(), gameType);
    ColorStatsResponse cachedData = cacheService.getCache(cacheKey, ColorStatsResponse.class);

    if (cachedData != null) {
      log.info("[Cache-Hit] ColorStats - userId={}, gameType={}", user.getId(), gameType);
      return cachedData;
    }

    log.info("[Cache-Miss] ColorStats - userId={}, gameType={}, DB 조회 시작", user.getId(), gameType);

    // Cache-Aside Pattern 2단계: 캐시 미스 시 DB에서 조회
    List<UserColorStat> userColorStats = userColorStatRepository.findByUserIdAndGameType(
        user.getId(), gameType);

    ColorStatsResponse response = new ColorStatsResponse(
        gameType, 0, 0, 0, 0, 0, 0, 0, 0
    );

    userColorStats.forEach(stat -> {
      switch (stat.getColor()) {
        case WHITE -> {
          response.setWhiteTotal(response.getWhiteTotal() + 1);
          switch (stat.getResult()) {
            case WIN -> response.setWhiteWins(response.getWhiteWins() + 1);
            case LOSE -> response.setWhiteLoses(response.getWhiteLoses() + 1);
            case DRAW -> response.setWhiteDraws(response.getWhiteDraws() + 1);
          }
        }
        case BLACK -> {
          response.setBlackTotal(response.getBlackTotal() + 1);
          switch (stat.getResult()) {
            case WIN -> response.setBlackWins(response.getBlackWins() + 1);
            case LOSE -> response.setBlackLoses(response.getBlackLoses() + 1);
            case DRAW -> response.setBlackDraws(response.getBlackDraws() + 1);
          }
        }
      }
    });

    // Cache-Aside Pattern 3단계: 조회 결과를 캐시에 저장 (TTL: 30분)
    cacheService.saveCache(cacheKey, response, 1800);
    log.info("[Cache-Set] ColorStats - userId={}, gameType={}, TTL=1800s", user.getId(), gameType);

    return response;
  }

  @Transactional(readOnly = true)
  public FirstMoveResponse getFirstMoveStats(User user, GameType gameType) {
    // Cache-Aside Pattern 1단계: 캐시에서 조회
    String cacheKey = buildFirstMoveCacheKey(user.getId(), gameType);
    FirstMoveResponse cachedData = cacheService.getCache(cacheKey, FirstMoveResponse.class);

    if (cachedData != null) {
      log.info("[Cache-Hit] FirstMove - userId={}, gameType={}", user.getId(), gameType);
      return cachedData;
    }

    log.info("[Cache-Miss] FirstMove - userId={}, gameType={}, DB 조회 시작", user.getId(), gameType);

    // Cache-Aside Pattern 2단계: 캐시 미스 시 DB에서 조회
    List<UserFirstMoveStat> whiteFirstMoves = userFirstMoveStatRepository.findByUserIdAndGameTypeAndColor(
        user.getId(), gameType, ChessColor.WHITE
    );

    List<UserFirstMoveStat> blackFirstMoves = userFirstMoveStatRepository.findByUserIdAndGameTypeAndColor(
        user.getId(), gameType, ChessColor.BLACK
    );

    Map<String, Integer> whiteMoves = new HashMap<>();
    Map<String, Integer> blackMoves = new HashMap<>();

    whiteFirstMoves.forEach(stat -> {
      whiteMoves.put(stat.getFirstMove(),
          whiteMoves.getOrDefault(stat.getFirstMove(), 0) + 1);
    });

    blackFirstMoves.forEach(stat -> {
      blackMoves.put(stat.getFirstMove(),
          blackMoves.getOrDefault(stat.getFirstMove(), 0) + 1);
    });

    FirstMoveResponse response = new FirstMoveResponse(gameType, whiteMoves, blackMoves);

    // Cache-Aside Pattern 3단계: 조회 결과를 캐시에 저장 (TTL: 30분)
    cacheService.saveCache(cacheKey, response, 1800);
    log.info("[Cache-Set] FirstMove - userId={}, gameType={}, TTL=1800s", user.getId(), gameType);

    return response;
  }

  @Transactional(readOnly = true)
  public UserPerfResponse getUserPerf(User user, GameType gameType) {
    // Cache-Aside Pattern 1단계: 캐시에서 조회
    String cacheKey = buildUserPerfCacheKey(user.getId(), gameType);
    UserPerfResponse cachedData = cacheService.getCache(cacheKey, UserPerfResponse.class);

    if (cachedData != null) {
      log.info("[Cache-Hit] UserPerf - userId={}, gameType={}", user.getId(), gameType);
      return cachedData;
    }

    log.info("[Cache-Miss] UserPerf - userId={}, gameType={}, DB 조회 시작", user.getId(), gameType);

    // Cache-Aside Pattern 2단계: 캐시 미스 시 DB에서 조회
    UserPerfResponse response = userPerfRepository.findByUserIdAndGameType(user.getId(), gameType)
        .map(userPerf -> new UserPerfResponse(
            userPerf.getRating(),
            userPerf.getGamesPlayed(),
            userPerf.isProv(),
            userPerf.getAll(),
            userPerf.getRated(),
            userPerf.getWins(),
            userPerf.getLosses(),
            userPerf.getDraws(),
            userPerf.getTour(),
            userPerf.getBerserk(),
            userPerf.getOpAvg(),
            userPerf.getSeconds(),
            userPerf.getDisconnects(),
            userPerf.getHighestRating(),
            userPerf.getLowestRating(),
            userPerf.getMaxStreak(),
            userPerf.getMaxLossStreak(),
            userPerf.getRated() < 50
        ))
        .orElse(null);

    if (response != null) {
      // Cache-Aside Pattern 3단계: 조회 결과를 캐시에 저장 (TTL: 1시간)
      cacheService.saveCache(cacheKey, response, 3600);
      log.info("[Cache-Set] UserPerf - userId={}, gameType={}, TTL=3600s", user.getId(), gameType);
    } else {
      log.info("[Cache-Miss-Null] UserPerf - userId={}, gameType={}, 데이터 없음", user.getId(), gameType);
    }

    return response;
  }

  public TierResponse getTierStats(User user, GameType gameType) {
    PerfsDto perfsDto = cacheService.getPerfs(user.getLichessId());

    if (perfsDto == null) {
      return null;
    }

    int rating;
    TierResult tierResult;

    switch (gameType) {
      case BULLET -> {
        rating = perfsDto.bullet().rating();
        tierResult = new TierResult(rating);
      }
      case BLITZ -> {
        rating = perfsDto.blitz().rating();
        tierResult = new TierResult(rating);
      }
      case RAPID -> {
        rating = perfsDto.rapid().rating();
        tierResult = new TierResult(rating);
      }
      case CLASSICAL -> {
        rating = perfsDto.classical().rating();
        tierResult = new TierResult(rating);
      }
      default -> throw new IllegalArgumentException("Unsupported game type: " + gameType);
    }

    return TierResponse.builder()
        .gameType(gameType)
        .rating(rating)
        .tierResult(tierResult)
        .build();
  }

  /**
   * ============================
   * Cache Key Builder Methods
   * ============================
   */
  private String buildYearStreakCacheKey(Long userId, Year year) {
    return String.format("stat:yearstreak:%d:%d", userId, year.getValue());
  }

  private String buildRatingHistoryCacheKey(Long userId, Year year) {
    return String.format("stat:ratinghistory:%d:%d", userId, year.getValue());
  }

  private String buildColorStatsCacheKey(Long userId, GameType gameType) {
    return String.format("stat:colorstats:%d:%s", userId, gameType.name());
  }

  private String buildFirstMoveCacheKey(Long userId, GameType gameType) {
    return String.format("stat:firstmove:%d:%s", userId, gameType.name());
  }

  private String buildUserPerfCacheKey(Long userId, GameType gameType) {
    return String.format("stat:userperf:%d:%s", userId, gameType.name()
        );
  }

}

