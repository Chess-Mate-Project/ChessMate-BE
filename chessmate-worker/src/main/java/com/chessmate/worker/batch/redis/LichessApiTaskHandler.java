package com.chessmate.worker.batch.redis;

import com.chessmate.common.service.UserBatchService;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.user.User;
import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.external.dto.account.LichessAccountDto;
import com.chessmate.external.dto.perf.UserPerfDto;
import com.chessmate.external.service.LichessApiService;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
import com.chessmate.worker.batch.service.UpdateDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LichessApiTaskHandler {

  private final LichessApiService lichessApiService;
  private final UserPerfRepository userPerfRepository;
  private final UserBatchService userBatchService;
  private final UpdateDataService updateDataService;
  private final UserRepositoryImpl userRepository;

  public void handle(LichessApiTask task) {
    if (task.type() == TaskType.PERF) {
      syncUserPerf(task);
    } else if (task.type() == TaskType.GAMES) {
      syncUserGames(task);
    } else if (task.type() == TaskType.ACCOUNT) {
      syncUserAccount(task);
    } else if (task.type() == TaskType.FORCE_UPDATE) {
      updateDataService.updateUserGameData(
          userRepository.findById(task.userId()).orElseThrow()
      );
    } else {
      log.warn("[Worker] 알 수 없는 작업 유형: {} for userId={}", task.type(), task.userId());
    }
  }

  /**
   * Account 정보 동기화
   * - DB에 저장된 데이터와 API 응답 데이터를 비교하여 다를 경우만 업데이트
   */
  private void syncUserAccount(LichessApiTask task) {
    log.info("[Worker] Account 정보 동기화 시작: userId={}, username={}", task.userId(), task.username());

    try {
      // Lichess API에서 Account 정보 조회
      LichessAccountDto accountDto = lichessApiService.getUserAccount(task.lichessToken());

      // DB에서 기존 사용자 정보 조회
      User existingUser = userRepository.findById(task.userId())
          .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + task.userId()));

      // 업데이트가 필요한지 확인
      boolean needsUpdate = false;

      // title 비교 및 업데이트
      if (!compareStrings(existingUser.getTitle(), accountDto.title())) {
        existingUser.setTitle(accountDto.title());
        needsUpdate = true;
        log.info("[Worker] Title 변경 감지: {} -> {}", existingUser.getTitle(), accountDto.title());
      }

      // 게임 통계 비교 및 업데이트
      if (existingUser.getAllGames() != accountDto.count().all()) {
        existingUser.setAllGames(accountDto.count().all());
        needsUpdate = true;
        log.info("[Worker] 전체 게임 수 변경: {} -> {}", existingUser.getAllGames(), accountDto.count().all());
      }

      if (existingUser.getRatedGames() != accountDto.count().rated()) {
        existingUser.setRatedGames(accountDto.count().rated());
        needsUpdate = true;
        log.info("[Worker] 레이티드 게임 수 변경: {} -> {}", existingUser.getRatedGames(), accountDto.count().rated());
      }

      if (existingUser.getWins() != accountDto.count().win()) {
        existingUser.setWins(accountDto.count().win());
        needsUpdate = true;
        log.info("[Worker] 승리 수 변경: {} -> {}", existingUser.getWins(), accountDto.count().win());
      }

      if (existingUser.getLosses() != accountDto.count().loss()) {
        existingUser.setLosses(accountDto.count().loss());
        needsUpdate = true;
        log.info("[Worker] 패배 수 변경: {} -> {}", existingUser.getLosses(), accountDto.count().loss());
      }

      if (existingUser.getDraws() != accountDto.count().draw()) {
        existingUser.setDraws(accountDto.count().draw());
        needsUpdate = true;
        log.info("[Worker] 무승부 수 변경: {} -> {}", existingUser.getDraws(), accountDto.count().draw());
      }

      if (existingUser.getTotalSeconds() != accountDto.playTime().total()) {
        existingUser.setTotalSeconds(accountDto.playTime().total());
        needsUpdate = true;
        log.info("[Worker] 총 플레이 시간 변경: {} -> {}", existingUser.getTotalSeconds(), accountDto.playTime().total());
      }

      // 변경사항이 있을 경우만 DB에 저장
      if (needsUpdate) {
        userRepository.save(existingUser);
        log.info("[Worker] Account 정보 업데이트 완료: userId={}", task.userId());
      } else {
        log.info("[Worker] Account 정보 변경 없음: userId={}", task.userId());
      }

    } catch (Exception e) {
      log.error("[Worker] Account 정보 동기화 실패 (userId={}): {}", task.userId(), e.getMessage(), e);
    }
  }

  /**
   * UserPerf(게임 타입별 통계) 동기화
   * - PERF 데이터를 4가지 게임 타입별로 조회하여 저장 또는 업데이트
   * - 기존 데이터와 비교하여 변경이 있을 때만 업데이트
   */
  private void syncUserPerf(LichessApiTask task) {
    log.info("[Worker] UserPerf 수집 시작: userId={}", task.userId());
    GameType[] gameTypes = {GameType.BULLET, GameType.BLITZ, GameType.RAPID, GameType.CLASSICAL};

    for (GameType type : gameTypes) {
      try {
        UserPerfDto dto = lichessApiService.getUserPerf(task.username(), type);

        int ratedCount = dto.stat().count().rated();
        int highestRating = dto.stat().highest() != null ? dto.stat().highest().int_() : 0;
        int lowestRating = dto.stat().lowest() != null ? dto.stat().lowest().int_() : 0;

        int maxWinStreak = dto.stat().resultStreak() != null &&
            dto.stat().resultStreak().win() != null &&
            dto.stat().resultStreak().win().max() != null
            ? dto.stat().resultStreak().win().max().v() : 0;

        int maxLossStreak = dto.stat().resultStreak() != null &&
            dto.stat().resultStreak().loss() != null &&
            dto.stat().resultStreak().loss().max() != null
            ? dto.stat().resultStreak().loss().max().v() : 0;

        // 기존 데이터 조회
        var existingPerf = userPerfRepository.findByUserIdAndGameType(task.userId(), type);

        // 기존 데이터가 있는 경우, 비교 로직 수행
        if (existingPerf.isPresent()) {
          UserPerf existing = existingPerf.get();
          boolean needsUpdate = false;

          // 레이팅 비교
          if (existing.getRating() != dto.perf().glicko().rating().intValue()) {
            log.info("[Worker] Rating 변경: {} -> {}", existing.getRating(), dto.perf().glicko().rating());
            needsUpdate = true;
          }

          // 게임 수 비교
          if (existing.getGamesPlayed() != dto.perf().nb()) {
            log.info("[Worker] GamesPlayed 변경: {} -> {}", existing.getGamesPlayed(), dto.perf().nb());
            needsUpdate = true;
          }

          // Prov 비교
          boolean newProv = dto.perf().glicko().provisional() != null && dto.perf().glicko().provisional();
          if (existing.isProv() != newProv) {
            log.info("[Worker] Prov 변경: {} -> {}", existing.isProv(), newProv);
            needsUpdate = true;
          }

          // 통계 비교
          if (existing.getAll() != dto.stat().count().all()) {
            log.info("[Worker] All 변경: {} -> {}", existing.getAll(), dto.stat().count().all());
            needsUpdate = true;
          }

          if (existing.getRated() != ratedCount) {
            log.info("[Worker] Rated 변경: {} -> {}", existing.getRated(), ratedCount);
            needsUpdate = true;
          }

          if (existing.getWins() != dto.stat().count().win()) {
            log.info("[Worker] Wins 변경: {} -> {}", existing.getWins(), dto.stat().count().win());
            needsUpdate = true;
          }

          if (existing.getLosses() != dto.stat().count().loss()) {
            log.info("[Worker] Losses 변경: {} -> {}", existing.getLosses(), dto.stat().count().loss());
            needsUpdate = true;
          }

          if (existing.getDraws() != dto.stat().count().draw()) {
            log.info("[Worker] Draws 변경: {} -> {}", existing.getDraws(), dto.stat().count().draw());
            needsUpdate = true;
          }

          if (existing.getTour() != dto.stat().count().tour()) {
            log.info("[Worker] Tour 변경: {} -> {}", existing.getTour(), dto.stat().count().tour());
            needsUpdate = true;
          }

          if (existing.getBerserk() != dto.stat().count().berserk()) {
            log.info("[Worker] Berserk 변경: {} -> {}", existing.getBerserk(), dto.stat().count().berserk());
            needsUpdate = true;
          }

          if (existing.getOpAvg() != dto.stat().count().opAvg()) {
            log.info("[Worker] OpAvg 변경: {} -> {}", existing.getOpAvg(), dto.stat().count().opAvg());
            needsUpdate = true;
          }

          if (existing.getSeconds() != dto.stat().count().seconds()) {
            log.info("[Worker] Seconds 변경: {} -> {}", existing.getSeconds(), dto.stat().count().seconds());
            needsUpdate = true;
          }

          if (existing.getDisconnects() != dto.stat().count().disconnects()) {
            log.info("[Worker] Disconnects 변경: {} -> {}", existing.getDisconnects(), dto.stat().count().disconnects());
            needsUpdate = true;
          }

          if (existing.getHighestRating() != highestRating) {
            log.info("[Worker] HighestRating 변경: {} -> {}", existing.getHighestRating(), highestRating);
            needsUpdate = true;
          }

          if (existing.getLowestRating() != lowestRating) {
            log.info("[Worker] LowestRating 변경: {} -> {}", existing.getLowestRating(), lowestRating);
            needsUpdate = true;
          }

          if (existing.getMaxStreak() != maxWinStreak) {
            log.info("[Worker] MaxStreak 변경: {} -> {}", existing.getMaxStreak(), maxWinStreak);
            needsUpdate = true;
          }

          if (existing.getMaxLossStreak() != maxLossStreak) {
            log.info("[Worker] MaxLossStreak 변경: {} -> {}", existing.getMaxLossStreak(), maxLossStreak);
            needsUpdate = true;
          }

          boolean newUncertain = ratedCount < 50;
          if (existing.isUncertain() != newUncertain) {
            log.info("[Worker] Uncertain 변경: {} -> {}", existing.isUncertain(), newUncertain);
            needsUpdate = true;
          }

          // 변경사항이 없으면 업데이트 스킵
          if (!needsUpdate) {
            log.info("[Worker] UserPerf 변경사항 없음: userId={}, gameType={}", task.userId(), type);
            continue;
          }

          log.info("[Worker] UserPerf 업데이트: userId={}, gameType={}", task.userId(), type);
        } else {
          log.info("[Worker] UserPerf 신규 저장: userId={}, gameType={}", task.userId(), type);
        }

        // 새 데이터 생성 및 저장
        UserPerf userPerf = UserPerf.builder()
            .userId(task.userId())
            .gameType(type)
            .rating(dto.perf().glicko().rating().intValue())
            .gamesPlayed(dto.perf().nb())
            .prov(dto.perf().glicko().provisional() != null && dto.perf().glicko().provisional())
            .all(dto.stat().count().all())
            .rated(ratedCount)
            .wins(dto.stat().count().win())
            .losses(dto.stat().count().loss())
            .draws(dto.stat().count().draw())
            .tour(dto.stat().count().tour())
            .berserk(dto.stat().count().berserk())
            .opAvg(dto.stat().count().opAvg())
            .seconds(dto.stat().count().seconds())
            .disconnects(dto.stat().count().disconnects())
            .highestRating(highestRating)
            .lowestRating(lowestRating)
            .maxStreak(maxWinStreak)
            .maxLossStreak(maxLossStreak)
            .uncertain(ratedCount < 50)
            .build();

        userPerfRepository.save(userPerf);
        log.info("[Worker] UserPerf 저장 완료: userId={} - {}", task.userId(), type);
      } catch (Exception e) {
        log.error("[Worker] UserPerf 수집 실패 (gameType={}): {}", type, e.getMessage());
      }
    }
  }

  /**
   * 게임 기록(Game) 동기화
   */
  private void syncUserGames(LichessApiTask task) {
    log.info("[Worker] GAMES 배치 실행: userId={}", task.userId());
    userBatchService.triggerUserUpdate(task.userId(), task.lichessToken(), task.isFullSync());
  }

  /**
   * 문자열 비교 (null 안전)
   */
  private boolean compareStrings(String existing, String incoming) {
    if (existing == null && incoming == null) {
      return true;
    }
    if (existing == null || incoming == null) {
      return false;
    }
    return existing.equals(incoming);
  }
}