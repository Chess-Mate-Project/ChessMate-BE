package com.chessmate.worker.batch.redis;

import com.chessmate.common.service.UserBatchService;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.external.dto.perf.UserPerfDto;
import com.chessmate.external.service.LichessApiService;
import com.chessmate.infra_redis.redis.dto.LichessApiTask;
import com.chessmate.infra_redis.redis.dto.TaskType;
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

  public void handle(LichessApiTask task) {
    if (task.type() == TaskType.PERF) {
      syncUserPerf(task);
    } else if (task.type() == TaskType.GAMES) {
      syncUserGames(task);
    }
  }

  private void syncUserPerf(LichessApiTask task) {
    log.info("[Worker] UserPerf 수집 시작: {}", task.userId());
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
        log.info("[Worker] UserPerf 저장 완료: {} - {}", task.userId(), type);
      } catch (Exception e) {
        log.error("[Worker] UserPerf 수집 실패 ({}): {}", type, e.getMessage());
      }
    }
  }


  private void syncUserGames(LichessApiTask task) {
    log.info("[Worker] GAMES 배치 실행: {}", task.userId());
    userBatchService.triggerUserUpdate(task.userId(), task.lichessToken());
  }
}