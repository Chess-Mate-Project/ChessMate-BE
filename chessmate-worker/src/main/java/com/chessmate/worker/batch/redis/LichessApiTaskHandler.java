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
import com.chessmate.worker.batch.service.BatchBarrierService;
import com.chessmate.worker.batch.service.UpdateDataService;
import com.chessmate.worker.batch.service.UpdateRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LichessApiTaskHandler {

  private final LichessApiService lichessApiService;
  private final UserPerfRepository userPerfRepository;
  private final UserBatchService userBatchService;
  private final UpdateDataService updateDataService;
  private final UserRepositoryImpl userRepository;
  private final BatchBarrierService batchBarrierService;
  private final UpdateRankingService updateRankingService;
  private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

  public void handle(LichessApiTask task) {
    if (task.type() == TaskType.RANKING_SNAPSHOT) {
      updateRankingService.buildAndCacheSnapshots(task.batchId());
      return;
    }

    if (task.type() == TaskType.PERF) {
      boolean ok = syncUserPerf(task);
      if (ok) {
        batchBarrierService.ackAndMaybeTriggerSnapshot(task.batchId(), task.taskId());
      }
      return;
    }

    if (task.type() == TaskType.GAMES) {
      syncUserGames(task);
      return;
    }

    if (task.type() == TaskType.ACCOUNT) {
      boolean ok = syncUserAccount(task);
      if (ok) {
        batchBarrierService.ackAndMaybeTriggerSnapshot(task.batchId(), task.taskId());
      }
      return;
    }

    if (task.type() == TaskType.FORCE_UPDATE) {
      updateDataService.updateUserGameData(
          userRepository.findById(task.userId()).orElseThrow(),
          null
      );
      return;
    }

    log.warn("[Worker] 알 수 없는 작업 유형: {} for userId={}", task.type(), task.userId());
  }

  /**
   * Account 정보 동기화
   * - DB에 저장된 데이터와 API 응답 데이터를 비교하여 다를 경우만 업데이트
   */
  private boolean syncUserAccount(LichessApiTask task) {
    log.info("[Worker] Account 정보 동기화 시작: userId={}, username={}", task.userId(), task.username());

    try {
      LichessAccountDto accountDto = lichessApiService.getUserAccount(task.lichessToken());

      transactionTemplate.executeWithoutResult(status -> {
        User existingUser = userRepository.findById(task.userId())
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + task.userId()));

        boolean needsUpdate = false;

        if (!compareStrings(existingUser.getTitle(), accountDto.title())) {
          existingUser.setTitle(accountDto.title());
          needsUpdate = true;
        }
        if (existingUser.getAllGames() != accountDto.count().all()) {
          existingUser.setAllGames(accountDto.count().all());
          needsUpdate = true;
        }
        if (existingUser.getRatedGames() != accountDto.count().rated()) {
          existingUser.setRatedGames(accountDto.count().rated());
          needsUpdate = true;
        }
        if (existingUser.getWins() != accountDto.count().win()) {
          existingUser.setWins(accountDto.count().win());
          needsUpdate = true;
        }
        if (existingUser.getLosses() != accountDto.count().loss()) {
          existingUser.setLosses(accountDto.count().loss());
          needsUpdate = true;
        }
        if (existingUser.getDraws() != accountDto.count().draw()) {
          existingUser.setDraws(accountDto.count().draw());
          needsUpdate = true;
        }
        if (existingUser.getTotalSeconds() != accountDto.playTime().total()) {
          existingUser.setTotalSeconds(accountDto.playTime().total());
          needsUpdate = true;
        }

        if (needsUpdate) {
          userRepository.save(existingUser);
        }
      });

      return true;
    } catch (Exception e) {
      log.error("[Worker] Account 정보 동기화 실패 (userId={}): {}", task.userId(), e.getMessage(), e);
      return false;
    }
  }

  private boolean syncUserPerf(LichessApiTask task) {
    log.info("[Worker] UserPerf 수집 시작: userId={}", task.userId());
    GameType[] gameTypes = {GameType.BULLET, GameType.BLITZ, GameType.RAPID, GameType.CLASSICAL};

    boolean allOk = true;

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

        transactionTemplate.executeWithoutResult(status -> {
          // 기존 UserPerf 조회 또는 새로 생성
          UserPerf existingPerf = userPerfRepository.findByUserIdAndGameType(task.userId(), type)
              .orElse(null);

          if (existingPerf != null) {
            log.info("[Worker-Update] UserPerf 업데이트: userId={}, gameType={}, oldRating={}, newRating={}",
                task.userId(), type, existingPerf.getRating(),
                dto.perf().glicko().rating().intValue());

            // 기존 데이터 업데이트
            existingPerf.setRating(dto.perf().glicko().rating().intValue());
            existingPerf.setGamesPlayed(dto.perf().nb());
            existingPerf.setProv(dto.perf().glicko().provisional() != null && dto.perf().glicko().provisional());
            existingPerf.setAll(dto.stat().count().all());
            existingPerf.setRated(ratedCount);
            existingPerf.setWins(dto.stat().count().win());
            existingPerf.setLosses(dto.stat().count().loss());
            existingPerf.setDraws(dto.stat().count().draw());
            existingPerf.setTour(dto.stat().count().tour());
            existingPerf.setBerserk(dto.stat().count().berserk());
            existingPerf.setOpAvg(dto.stat().count().opAvg());
            existingPerf.setSeconds(dto.stat().count().seconds());
            existingPerf.setDisconnects(dto.stat().count().disconnects());
            existingPerf.setHighestRating(highestRating);
            existingPerf.setLowestRating(lowestRating);
            existingPerf.setMaxStreak(maxWinStreak);
            existingPerf.setMaxLossStreak(maxLossStreak);
            existingPerf.setUncertain(ratedCount < 50);

            userPerfRepository.save(existingPerf);
          } else {
            log.info("[Worker-Create] UserPerf 신규 생성: userId={}, gameType={}, rating={}",
                task.userId(), type, dto.perf().glicko().rating().intValue());

            // 신규 생성
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
          }
        });

      } catch (Exception e) {
        allOk = false;
        log.error("[Worker] UserPerf 수집 실패 (gameType={}): {}", type, e.getMessage(), e);
      }
    }

    return allOk;
  }

  private void syncUserGames(LichessApiTask task) {
    log.info("[Worker] GAMES 배치 실행: userId={}", task.userId());
    userBatchService.triggerUserUpdate(task.userId(), task.lichessToken(), task.isFullSync());
  }

  private boolean compareStrings(String existing, String incoming) {
    if (existing == null && incoming == null) return true;
    if (existing == null || incoming == null) return false;
    return existing.equals(incoming);
  }
}