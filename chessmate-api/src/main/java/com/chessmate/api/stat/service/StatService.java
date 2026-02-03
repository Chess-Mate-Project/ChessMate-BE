package com.chessmate.api.stat.service;

import com.chessmate.api.redis.LichessApiProducer;
import com.chessmate.api.stat.dto.ColorStatsResponse;
import com.chessmate.api.stat.dto.DailyStreakDto;
import com.chessmate.api.stat.dto.FirstMoveResponse;
import com.chessmate.api.stat.dto.TierResponse;
import com.chessmate.api.stat.dto.UserPerfResponse;
import com.chessmate.api.stat.dto.YearStreakDto;
import com.chessmate.common.dto.TierResult;
import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameResult;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.user.User;
import com.chessmate.domain.userColorStat.UserColorStat;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.external.dto.account.PerfsDto;
import com.chessmate.external.service.LichessApiService;
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

    List<DailyStreakDto> dailyStreakDto = new ArrayList<>();

    LocalDate start = LocalDate.of(year.getValue(), 1, 1);
    LocalDate end = LocalDate.of(year.getValue(), 12, 31);

    userDailyStreakRepository.findByUserIdAndYearRange(user.getId(), start, end)
        .forEach(streak -> {

              DailyStreakDto dto = new DailyStreakDto(
                  streak.getDate(),
                  streak.getWin(),
                  streak.getLose(),
                  streak.getDraw(),
                  (streak.getWin() + streak.getLose() + streak.getDraw())
              );

              dailyStreakDto.add(dto);

            }
        );

    return new YearStreakDto(year, dailyStreakDto);
  }

  @Transactional(readOnly = true)
  public ColorStatsResponse getColorStats(User user, GameType gameType) {
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
            case WIN:
              response.setWhiteWins(response.getWhiteWins() + 1);
              break;
            case LOSE:
              response.setWhiteLoses(response.getWhiteLoses() + 1);
              break;
            case DRAW:
              response.setWhiteDraws(response.getWhiteDraws() + 1);
              break;
          }
        }
        case BLACK -> {
          response.setBlackTotal(response.getBlackTotal() + 1);
          switch (stat.getResult()) {
            case WIN:
              response.setBlackWins(response.getBlackWins() + 1);
              break;
            case LOSE:
              response.setBlackLoses(response.getBlackLoses() + 1);
              break;
            case DRAW:
              response.setBlackDraws(response.getBlackDraws() + 1);
              break;
          }
        }
      }
    });

    return response;
  }

  public FirstMoveResponse getFirstMoveStats(User user, GameType gameType) {
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

    return new FirstMoveResponse(gameType, whiteMoves, blackMoves);
  }

  public TierResponse getTierStats(User user, GameType gameType) {
    PerfsDto perfsDto = cacheService.getPerfs(user.getLichessId());

    if(perfsDto == null) {

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
   * 게임 타입별 상세 퍼포먼스 정보 조회
   * - UserPerf에서 게임 타입별 모든 통계 정보 반환
   * @param user 사용자
   * @param gameType 게임 타입
   * @return UserPerfResponse 게임 타입별 상세 퍼포먼스 정보
   */
  @Transactional(readOnly = true)
  public UserPerfResponse getUserPerf(User user, GameType gameType) {

    return userPerfRepository.findByUserIdAndGameType(user.getId(), gameType)
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
            userPerf.getRated() < 50 // 불확실성 판단
        ))
        .orElse(null);  // UserPerf 데이터 없으면 null 반환
  }

}
