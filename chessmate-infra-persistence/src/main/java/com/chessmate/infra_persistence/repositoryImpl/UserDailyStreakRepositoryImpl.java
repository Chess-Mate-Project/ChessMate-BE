package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.domain.userDailyStreak.UserDailyStreakRepository;
import com.chessmate.infra_persistence.entity.UserDailyStreakEntity;
import com.chessmate.infra_persistence.jpaRepository.UserDailyStreakJpaRepository;
import com.chessmate.infra_persistence.mapper.UserDailyStreakMapper;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserDailyStreakRepositoryImpl implements UserDailyStreakRepository {

  private final UserDailyStreakJpaRepository jpaRepository;

  @Override
  public UserDailyStreak save(UserDailyStreak userDailyStreak) {
    UserDailyStreakEntity entity = UserDailyStreakMapper.toEntity(userDailyStreak);
    UserDailyStreakEntity saved = jpaRepository.save(entity);
    return UserDailyStreakMapper.toDomain(saved);
  }

  @Override
  public Optional<UserDailyStreak> findByUserIdAndDate(Long userId, LocalDate date) {
    return Optional.empty();
  }
}
