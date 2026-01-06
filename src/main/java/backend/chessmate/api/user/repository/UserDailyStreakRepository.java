package backend.chessmate.api.user.repository;

import backend.chessmate.api.user.entity.UserColorStat;
import backend.chessmate.api.user.entity.UserDailyStreak;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyStreakRepository extends JpaRepository<UserDailyStreak, Long> {
  Optional<UserDailyStreak> findByUserIdAndDate(Long userId, LocalDate date);
}