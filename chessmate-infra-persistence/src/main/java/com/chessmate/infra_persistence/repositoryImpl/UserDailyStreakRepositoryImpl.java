package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.domain.userDailyStreak.UserDailyStreak;
import com.chessmate.domain.userDailyStreak.UserDailyStreakRepository;
import com.chessmate.infra_persistence.entity.UserDailyStreakEntity;
import com.chessmate.infra_persistence.jpaRepository.UserDailyStreakJpaRepository;
import com.chessmate.infra_persistence.mapper.UserDailyStreakMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
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
    return jpaRepository.findByUserIdAndDate(userId, date)
        .map(UserDailyStreakMapper::toDomain);
  }

  @Override
  public Long findLastGameAtByUserId(Long userId) {
    return jpaRepository.findLastGameAtByUserId(userId).orElse(0L);
  }

  @Override
  public List<UserDailyStreak> findByUserIdAndYearRange(Long userId, LocalDate start,
      LocalDate end) {
    log.debug("[UserDailyStreakRepository] DB 조회 시작 - userId={}, start={}, end={}", userId, start, end);
    List<UserDailyStreakEntity> entities = jpaRepository.findByUserIdAndDateBetween(userId, start, end);
    log.info("[UserDailyStreakRepository] DB 조회 완료 - userId={}, start={}, end={}, 조회된 데이터 개수={}",
        userId, start, end, entities.size());

    if (!entities.isEmpty()) {
      entities.forEach(entity ->
          log.debug("[UserDailyStreakRepository] Entity 상세 - id={}, userId={}, date={}, win={}, lose={}, draw={}, lastGameAt={}, lastRating={}",
              entity.getId(), entity.getUserId(), entity.getDate(), entity.getWin(),
              entity.getLose(), entity.getDraw(), entity.getLastGameAt(), entity.getLastRating())
      );
    }

    return entities.stream()
        .map(UserDailyStreakMapper::toDomain)
        .toList();
  }

  public List<UserDailyStreak> findByUserId(Long userId) {
    return jpaRepository.findAllByUserId(userId).stream()
        .map(UserDailyStreakMapper::toDomain)
        .toList();
  }
}
