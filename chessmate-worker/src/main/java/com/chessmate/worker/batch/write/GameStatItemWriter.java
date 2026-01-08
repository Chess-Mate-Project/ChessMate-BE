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
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@StepScope
public class GameStatItemWriter implements ItemWriter<GameStat> {

  private final UserDailyStreakRepositoryImpl streakRepository;
  private final UserColorStatRepositoryImpl colorStatRepository;
  private final UserFirstMoveStatRepositoryImpl firstMoveStatRepository;

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
            }
            streakRepository.save(existing);
          }, () -> streakRepository.save(aggregated));
    }

    if (!colorStats.isEmpty()) {
      colorStatRepository.saveAll(colorStats);
    }
    if (!firstMoveStats.isEmpty()) {
      firstMoveStatRepository.saveAll(firstMoveStats);
    }
  }
}
