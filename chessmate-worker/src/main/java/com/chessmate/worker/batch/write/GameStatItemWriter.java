package com.chessmate.worker.batch.write;

import com.chessmate.domain.userColorStat.UserColorStat;
import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.infra_persistence.repositoryImpl.UserColorStatRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserDailyStreakRepositoryImpl;
import com.chessmate.infra_persistence.repositoryImpl.UserFirstMoveStatRepositoryImpl;
import com.chessmate.worker.batch.dto.GameStat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@StepScope
@Slf4j
public class GameStatItemWriter implements ItemWriter<GameStat> {

  private final UserDailyStreakRepositoryImpl streakRepository;
  private final UserColorStatRepositoryImpl colorStatRepository;
  private final UserFirstMoveStatRepositoryImpl firstMoveStatRepository;

  @Value("#{jobParameters['batchId']}")
  private String batchId;

  @Override
  public void write(Chunk<? extends GameStat> chunk) throws Exception {

    List<UserColorStat> colorStats = new ArrayList<>();
    List<UserFirstMoveStat> firstMoveStats = new ArrayList<>();

    // userId|date -> aggregated streak
    Map<String, UserDailyStreak> aggregatedStreaks = new HashMap<>();

    for (GameStat gs : chunk) {

      UserDailyStreak streak = gs.dailyStreak();
      if (streak != null) {
        String key = streak.getUserId() + "|" + streak.getDate().toString();
        UserDailyStreak agg = aggregatedStreaks.get(key);
        if (agg == null) {
          aggregatedStreaks.put(key, streak);
        } else {
          agg.setWin(agg.getWin() + streak.getWin());
          agg.setLose(agg.getLose() + streak.getLose());
          agg.setDraw(agg.getDraw() + streak.getDraw());
          if (streak.getLastGameAt() != null && (agg.getLastGameAt() == null
              || streak.getLastGameAt() > agg.getLastGameAt())) {
            agg.setLastGameAt(streak.getLastGameAt());
            agg.setLastRating(streak.getLastRating());
          }
        }
      }

      if (gs.colorStat() != null) {
        colorStats.add(gs.colorStat());
      }
      if (gs.firstMoveStat() != null) {
        firstMoveStats.add(gs.firstMoveStat());
      }
    }
    log.info("[Batch-Writer] [START] 배치 데이터 저장 시작 - batchId={}, aggregatedStreaks={}개, colorStats={}개, firstMoveStats={}개",
        batchId, aggregatedStreaks.size(), colorStats.size(), firstMoveStats.size());

    // 집계된 스트릭 데이터 상세 로깅
    log.info("[Batch-Writer] [AGGREGATED-DATA] 집계된 일일 스트릭 목록:");
    for (Map.Entry<String, UserDailyStreak> entry : aggregatedStreaks.entrySet()) {
      UserDailyStreak streak = entry.getValue();
      log.info("  - userId={}, date={}, win={}, lose={}, draw={}, lastGameAt={}, lastRating={}",
          streak.getUserId(), streak.getDate(), streak.getWin(), streak.getLose(), streak.getDraw(),
          streak.getLastGameAt(), streak.getLastRating());
    }

    // 집계된 streak들을 DB에 반영 (존재하면 업데이트, 없으면 삽입)
    for (UserDailyStreak aggregated : aggregatedStreaks.values()) {
      streakRepository.findByUserIdAndDate(aggregated.getUserId(), aggregated.getDate())
          .ifPresentOrElse(existing -> {
            existing.setWin(existing.getWin() + aggregated.getWin());
            existing.setLose(existing.getLose() + aggregated.getLose());
            existing.setDraw(existing.getDraw() + aggregated.getDraw());
            if (aggregated.getLastGameAt() != null && (existing.getLastGameAt() == null
                || aggregated.getLastGameAt() > existing.getLastGameAt())) {
              existing.setLastGameAt(aggregated.getLastGameAt());
              // lastGameAt이 더 최신이면 lastRating도 함께 업데이트
              existing.setLastRating(aggregated.getLastRating());
              log.debug("[Batch-Writer] [STREAK-UPDATE] batchId={}, userId={}, date={}, lastRating={}",
                  batchId, existing.getUserId(), existing.getDate(), existing.getLastRating());
            }
            streakRepository.save(existing);
          }, () -> {
            streakRepository.save(aggregated);
            log.debug("[Batch-Writer] [STREAK-INSERT] batchId={}, userId={}, date={}, lastRating={}",
                batchId, aggregated.getUserId(), aggregated.getDate(), aggregated.getLastRating());
          });
    }

    if (!colorStats.isEmpty()) {
      colorStatRepository.saveAll(colorStats);
      log.debug("[Batch-Writer] [COLORSTAT-SAVE] batchId={}, count={}", batchId, colorStats.size());
    }
    if (!firstMoveStats.isEmpty()) {
      firstMoveStatRepository.saveAll(firstMoveStats);
      log.debug("[Batch-Writer] [FIRSTMOVE-SAVE] batchId={}, count={}", batchId, firstMoveStats.size());
    }

    log.info("[Batch-Writer] [COMPLETE] 배치 데이터 저장 완료 - batchId={}, aggregatedStreaks={}개",
        batchId, aggregatedStreaks.size());
  }
}
