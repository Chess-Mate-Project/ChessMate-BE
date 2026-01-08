package com.chessmate.domain.userDailyStreak;


import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public interface UserDailyStreakRepository {

  UserDailyStreak save(UserDailyStreak userDailyStreak);

  Optional<UserDailyStreak> findByUserIdAndDate(Long userId, LocalDate date);

  Long findLastGameAtByUserId(Long userId);
}