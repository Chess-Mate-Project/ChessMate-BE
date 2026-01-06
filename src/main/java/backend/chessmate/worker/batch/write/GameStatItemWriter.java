package backend.chessmate.worker.batch.write;

import backend.chessmate.api.user.entity.UserColorStat;
import backend.chessmate.api.user.entity.UserDailyStreak;
import backend.chessmate.api.user.entity.UserFirstMoveStat;
import backend.chessmate.api.user.repository.UserColorStatRepository;
import backend.chessmate.api.user.repository.UserDailyStreakRepository;
import backend.chessmate.api.user.repository.UserFirstMoveStatRepository;
import backend.chessmate.worker.batch.dto.GameStat;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@StepScope
public class GameStatItemWriter implements ItemWriter<GameStat> {

  private final UserDailyStreakRepository streakRepository;
  private final UserColorStatRepository colorStatRepository;
  private final UserFirstMoveStatRepository firstMoveStatRepository;



  @Override
  public void write(Chunk<? extends GameStat> chunk) throws Exception {

    List<UserColorStat> colorStats = new ArrayList<>();
    List<UserFirstMoveStat> firstMoveStats = new ArrayList<>();

    for (GameStat gs : chunk) {

      UserDailyStreak streak = gs.dailyStreak();
      streakRepository.findByUserIdAndDate(streak.getUserId(), streak.getDate())
          .ifPresentOrElse(existing -> {
            existing.setWin(existing.getWin() + streak.getWin());
            existing.setLose(existing.getLose() + streak.getLose());
            existing.setDraw(existing.getDraw() + streak.getDraw());
            existing.setLastGameAt(streak.getLastGameAt());
            streakRepository.save(existing);
          }, () -> streakRepository.save(streak));

      colorStats.add(gs.colorStat());

      firstMoveStats.add(gs.firstMoveStat());
    }

    if (!colorStats.isEmpty()) colorStatRepository.saveAll(colorStats);
    if (!firstMoveStats.isEmpty()) firstMoveStatRepository.saveAll(firstMoveStats);
  }
}
