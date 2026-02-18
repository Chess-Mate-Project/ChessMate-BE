package com.chessmate.worker.batch.redis;

import com.chessmate.common.service.UserBatchService;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.user.User;
import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.domain.userDailyStreak.UserDailyStreakRepository;
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
  private final UserDailyStreakRepository userDailyStreakRepository;
  private final UpdateRankingService updateRankingService;
  private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

  public void handle(LichessApiTask task) {
    log.info("[Worker-Handler] 작업 처리 시작 - taskId={}, userId={}, taskType={}",
        task.taskId(), task.userId(), task.type());

    if (task.type() == TaskType.RANKING_SNAPSHOT) {
      log.info("[Worker-Handler] RANKING_SNAPSHOT 작업 처리 - batchId={}", task.batchId());
      updateRankingService.buildAndCacheSnapshots(task.batchId());
      log.info("[Worker-Handler] RANKING_SNAPSHOT 작업 완료");
      return;
    }

    if (task.type() == TaskType.PERF) {
      log.info("[Worker-Handler] PERF 작업 처리 - userId={}, username={}", task.userId(), task.username());
      boolean ok = syncUserPerf(task);
      log.info("[Worker-Handler] PERF 작업 결과 - status={}", ok ? "SUCCESS" : "FAILED");
      if (ok) {
        batchBarrierService.ackAndMaybeTriggerSnapshot(task.batchId(), task.taskId());
      }
      return;
    }

    if (task.type() == TaskType.GAMES) {
      log.info("[Worker-Handler] GAMES 작업 처리 - userId={}", task.userId());
      syncUserGames(task);
      log.info("[Worker-Handler] GAMES 작업 완료");
      return;
    }

    if (task.type() == TaskType.ACCOUNT) {
      log.info("[Worker-Handler] ACCOUNT 작업 처리 - userId={}, username={}", task.userId(), task.username());
      boolean ok = syncUserAccount(task);
      log.info("[Worker-Handler] ACCOUNT 작업 결과 - status={}", ok ? "SUCCESS" : "FAILED");
      if (ok) {
        batchBarrierService.ackAndMaybeTriggerSnapshot(task.batchId(), task.taskId());
      }
      return;
    }

    if (task.type() == TaskType.FORCE_UPDATE) {
      log.info("[Worker-Handler] FORCE_UPDATE 작업 처리 - userId={}", task.userId());
      updateDataService.updateUserGameData(
          userRepository.findById(task.userId()).orElseThrow(),
          null
      );
      log.info("[Worker-Handler] FORCE_UPDATE 작업 완료");
      return;
    }

    log.warn("[Worker-Handler] === 알 수 없는 작업 유형 === taskType={}, userId={}", task.type(), task.userId());
  }

  /**
   * Account 정보 동기화
   * - DB에 저장된 데이터와 API 응답 데이터를 비교하여 다를 경우만 업데이트
   */
  private boolean syncUserAccount(LichessApiTask task) {
    log.info("[Worker-ACCOUNT] ========== Account 정보 동기화 시작 ==========");
    log.info("[Worker-ACCOUNT] 사용자: userId={}, username={}", task.userId(), task.username());

    try {
      // 1. API에서 데이터 조회
      log.debug("[Worker-ACCOUNT] API 호출: username={}", task.username());
      LichessAccountDto accountDto = lichessApiService.getUserAccount(task.lichessToken());

      log.debug("[Worker-ACCOUNT] API 응답 수신: title={}, allGames={}, ratedGames={}, wins={}, losses={}, draws={}, seconds={}",
          accountDto.title(), accountDto.count().all(), accountDto.count().rated(),
          accountDto.count().win(), accountDto.count().loss(), accountDto.count().draw(),
          accountDto.playTime().total());

      transactionTemplate.executeWithoutResult(status -> {
        // 2. DB에서 사용자 조회
        log.debug("[Worker-ACCOUNT] DB 조회: userId={}", task.userId());

        User existingUser = userRepository.findById(task.userId())
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + task.userId()));

        log.info("[Worker-ACCOUNT] DB 기존 데이터: title={}, allGames={}, ratedGames={}, wins={}, losses={}, draws={}, seconds={}",
            existingUser.getTitle(), existingUser.getAllGames(), existingUser.getRatedGames(),
            existingUser.getWins(), existingUser.getLosses(), existingUser.getDraws(),
            existingUser.getTotalSeconds());

        // 3. 각 필드별 변경 감지
        boolean needsUpdate = false;
        StringBuilder changeLog = new StringBuilder();
        changeLog.append("[변경 사항]\n");

        if (!compareStrings(existingUser.getTitle(), accountDto.title())) {
          changeLog.append(String.format("  - title: '%s' -> '%s'\n", existingUser.getTitle(), accountDto.title()));
          existingUser.setTitle(accountDto.title());
          needsUpdate = true;
        }
        if (existingUser.getAllGames() != accountDto.count().all()) {
          changeLog.append(String.format("  - allGames: %d -> %d\n", existingUser.getAllGames(), accountDto.count().all()));
          existingUser.setAllGames(accountDto.count().all());
          needsUpdate = true;
        }
        if (existingUser.getRatedGames() != accountDto.count().rated()) {
          changeLog.append(String.format("  - ratedGames: %d -> %d\n", existingUser.getRatedGames(), accountDto.count().rated()));
          existingUser.setRatedGames(accountDto.count().rated());
          needsUpdate = true;
        }
        if (existingUser.getWins() != accountDto.count().win()) {
          changeLog.append(String.format("  - wins: %d -> %d\n", existingUser.getWins(), accountDto.count().win()));
          existingUser.setWins(accountDto.count().win());
          needsUpdate = true;
        }
        if (existingUser.getLosses() != accountDto.count().loss()) {
          changeLog.append(String.format("  - losses: %d -> %d\n", existingUser.getLosses(), accountDto.count().loss()));
          existingUser.setLosses(accountDto.count().loss());
          needsUpdate = true;
        }
        if (existingUser.getDraws() != accountDto.count().draw()) {
          changeLog.append(String.format("  - draws: %d -> %d\n", existingUser.getDraws(), accountDto.count().draw()));
          existingUser.setDraws(accountDto.count().draw());
          needsUpdate = true;
        }
        if (existingUser.getTotalSeconds() != accountDto.playTime().total()) {
          changeLog.append(String.format("  - totalSeconds: %d -> %d\n", existingUser.getTotalSeconds(), accountDto.playTime().total()));
          existingUser.setTotalSeconds(accountDto.playTime().total());
          needsUpdate = true;
        }

        // 4. 업데이트 여부 판단
        if (needsUpdate) {
          log.info("[Worker-ACCOUNT] === 데이터 변경 감지 ===\n{}", changeLog);
          log.debug("[Worker-ACCOUNT] UPDATE 실행 중...");
          userRepository.save(existingUser);
          log.info("[Worker-ACCOUNT] === UPDATE 완료 ===");
        } else {
          log.info("[Worker-ACCOUNT] 데이터 변경 없음 - 스킵");
        }
      });

      log.info("[Worker-ACCOUNT] ========== Account 동기화 완료 (SUCCESS) ==========");
      return true;
    } catch (Exception e) {
      log.error("[Worker-ACCOUNT] === Account 동기화 실패 === userId={}, message={}",
          task.userId(), e.getMessage(), e);
      return false;
    }
  }

  private boolean syncUserPerf(LichessApiTask task) {
    log.info("[Worker-PERF] ========== UserPerf 동기화 시작 ==========");
    log.info("[Worker-PERF] 사용자: userId={}, username={}", task.userId(), task.username());

    GameType[] gameTypes = {GameType.BULLET, GameType.BLITZ, GameType.RAPID, GameType.CLASSICAL};
    boolean allOk = true;

    for (GameType type : gameTypes) {
      log.debug("[Worker-PERF] [{}] API 호출 시작", type);

      try {
        // 1. API에서 데이터 조회
        UserPerfDto dto = lichessApiService.getUserPerf(task.username(), type);

        log.debug("[Worker-PERF] [{}] API 응답 수신: rating={}, gamesPlayed={}, rated={}",
            type,
            dto.perf().glicko().rating().intValue(),
            dto.perf().nb(),
            dto.stat().count().rated());

        int ratedCount = dto.stat().count().rated();
        int newRating = dto.perf().glicko().rating().intValue();
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

        // 2. DB에서 기존 데이터 조회
        log.debug("[Worker-PERF] [{}] DB 조회: userId={}, gameType={}", type, task.userId(), type);

        transactionTemplate.executeWithoutResult(status -> {
          // DB 조회: 기존 데이터 있는지 확인
          log.info("[Worker-PERF] [{}] === DB 조회 시작 === userId={}, gameType={}",
              type, task.userId(), type);
          long findStartTime = System.currentTimeMillis();

          UserPerf existingPerf = userPerfRepository.findByUserIdAndGameType(task.userId(), type)
              .orElse(null);

          long findDuration = System.currentTimeMillis() - findStartTime;

          // 조회 결과 로그 - 중복 생성 감지용
          if (existingPerf != null) {
            log.info("[Worker-PERF] [{}] [DB-FOUND] 기존 데이터 발견 | id={}, rating={}, games={}, rated={}, QueryTime={}ms",
                type, existingPerf.getId(), existingPerf.getRating(),
                existingPerf.getGamesPlayed(), existingPerf.getRated(), findDuration);
          } else {
            log.warn("[Worker-PERF] [{}] [DB-NOT-FOUND] 기존 데이터 없음 | userId={}, gameType={} | 신규 INSERT 예정 | QueryTime={}ms",
                type, task.userId(), type, findDuration);
          }

          if (existingPerf != null) {
            // 3-1. UPDATE 케이스
            long updateStartTime = System.currentTimeMillis();
            log.info("[Worker-PERF] [{}] [UPDATE] 시작 | id={}", type, existingPerf.getId());

            // 변경 사항 확인
            boolean ratingChanged = existingPerf.getRating() != newRating;
            boolean gamesChanged = existingPerf.getGamesPlayed() != dto.perf().nb();
            boolean ratedChanged = existingPerf.getRated() != ratedCount;

            if (ratingChanged || gamesChanged || ratedChanged) {
              log.info("[Worker-PERF] [{}] [UPDATE-CHANGE-DETECTED] id={} | 변경 사항 감지",
                  type, existingPerf.getId());
              if (ratingChanged) {
                log.info("[Worker-PERF] [{}]   [FIELD-CHANGE] rating: {} → {}",
                    type, existingPerf.getRating(), newRating);
              }
              if (gamesChanged) {
                log.info("[Worker-PERF] [{}]   [FIELD-CHANGE] games: {} → {}",
                    type, existingPerf.getGamesPlayed(), dto.perf().nb());
              }
              if (ratedChanged) {
                log.info("[Worker-PERF] [{}]   [FIELD-CHANGE] rated: {} → {}",
                    type, existingPerf.getRated(), ratedCount);
              }
            } else {
              log.info("[Worker-PERF] [{}] [UPDATE-SKIP] id={} | 변경 사항 없음 - 스킵",
                  type, existingPerf.getId());
              return;
            }

            // 기존 데이터 업데이트
            existingPerf.setRating(newRating);
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

            log.debug("[Worker-PERF] [{}] [UPDATE-EXECUTE] id={} | 저장 중...", type, existingPerf.getId());
            UserPerf updatedPerf = userPerfRepository.save(existingPerf);
            long updateDuration = System.currentTimeMillis() - updateStartTime;

            log.info("[Worker-PERF] [{}] [UPDATE-SUCCESS] ✓ | id={}, rating={}, games={}, SaveTime={}ms",
                type, updatedPerf.getId(), updatedPerf.getRating(),
                updatedPerf.getGamesPlayed(), updateDuration);

          } else {
            // 3-2. INSERT 케이스
            long insertStartTime = System.currentTimeMillis();
            log.info("[Worker-PERF] [{}] [INSERT] 시작 | userId={}", type, task.userId());
            log.debug("[Worker-PERF] [{}] [INSERT-DATA] rating={}, games={}, rated={}, wins={}, losses={}, draws={}",
                type, newRating, dto.perf().nb(), ratedCount,
                dto.stat().count().win(), dto.stat().count().loss(), dto.stat().count().draw());

            UserPerf userPerf = UserPerf.builder()
                .userId(task.userId())
                .gameType(type)
                .rating(newRating)
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

            log.debug("[Worker-PERF] [{}] [INSERT-EXECUTE] userId={} | 저장 중...", type, task.userId());
            UserPerf savedPerf = userPerfRepository.save(userPerf);
            long insertDuration = System.currentTimeMillis() - insertStartTime;

            log.info("[Worker-PERF] [{}] [INSERT-SUCCESS] ✓ | newId={}, userId={}, gameType={}, rating={}, SaveTime={}ms",
                type, savedPerf.getId(), savedPerf.getUserId(), savedPerf.getGameType(),
                savedPerf.getRating(), insertDuration);
          }
        });

      } catch (Exception e) {
        allOk = false;
        log.error("[Worker-PERF] [{}] [ERROR] ✗ | userId={}, gameType={}, message={}, exceptionType={}",
            type, task.userId(), type, e.getMessage(), e.getClass().getSimpleName(), e);
      }
    }

    log.info("[Worker-PERF] ========== UserPerf 동기화 완료 (status={}) ==========", allOk ? "SUCCESS" : "PARTIAL_FAILURE");
    return allOk;
  }

  private void syncUserGames(LichessApiTask task) {
    log.info("[Worker-GAMES] ========== 게임 데이터 동기화 시작 ==========");
    log.info("[Worker-GAMES] 사용자: userId={}, isFullSync={}", task.userId(), task.isFullSync());
    try {
      userBatchService.triggerUserUpdate(task.userId(), task.lichessToken(), task.isFullSync());
      log.info("[Worker-GAMES] ========== 게임 데이터 동기화 완료 (SUCCESS) ==========");

      // 다음 배치를 위한 since 값 로깅
      Long lastGameAt = userDailyStreakRepository.findLastGameAtByUserId(task.userId());
      if (lastGameAt != null && lastGameAt > 0) {
        log.info("[Worker-GAMES] [NEXT-SINCE] 다음 배치 조회 기준: lastGameAt={}, nextSince={} ({}시간 전)",
            lastGameAt, lastGameAt + 1, (System.currentTimeMillis() - lastGameAt) / (1000.0 * 60 * 60));
      }
    } catch (Exception e) {
      log.error("[Worker-GAMES] ========== 게임 데이터 동기화 실패 ========== userId={}, message={}",
          task.userId(), e.getMessage(), e);
    }
  }

  /**
   * 문자열 비교 헬퍼 메서드
   * - null 안전성 처리 포함
   */
  private boolean compareStrings(String existing, String incoming) {
    if (existing == null && incoming == null) return true;
    if (existing == null || incoming == null) return false;
    return existing.equals(incoming);
  }
}