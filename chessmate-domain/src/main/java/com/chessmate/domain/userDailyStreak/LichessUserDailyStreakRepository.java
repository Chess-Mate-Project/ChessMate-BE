package com.chessmate.domain.userDailyStreak;


import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LichessUserDailyStreakRepository {

  LichessUserDailyStreak save(LichessUserDailyStreak lichessUserDailyStreak);

  Optional<LichessUserDailyStreak> findByUserIdAndDate(Long userId, LocalDate date);

  Long findLastGameAtByUserId(Long userId);

  List<LichessUserDailyStreak> findByUserIdAndYearRange(Long userId, LocalDate start, LocalDate end);

  void deleteAllByUserId(Long userId);
}

