package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.infra_persistence.entity.UserDailyStreakEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDailyStreakJpaRepository extends
    JpaRepository<UserDailyStreakEntity, Long> {

  @Query("""
  SELECT uds.lastGameAt
  FROM UserDailyStreakEntity uds
  WHERE uds.userId = :userId
  ORDER BY uds.lastGameAt ASC
  LIMIT 1
""")
  Optional<Long> findLastGameAtByUserId(@Param("userId") Long userId);

  Optional<UserDailyStreakEntity> findByUserIdAndDate(Long userId, LocalDate date);

  List<UserDailyStreakEntity> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);
}
