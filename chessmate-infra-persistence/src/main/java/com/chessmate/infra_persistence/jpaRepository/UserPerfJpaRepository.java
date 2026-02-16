package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.common.type.GameType;
import com.chessmate.infra_persistence.entity.UserPerfEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserPerfJpaRepository extends JpaRepository<UserPerfEntity, Long> {
  Optional<UserPerfEntity> findByUserIdAndGameType(Long userId, GameType gameType);

  void deleteByUserId(Long userId);

  /**
   * Pessimistic Lock을 사용한 조회
   * - 조회 시점부터 해당 row에 Write Lock 획득
   * - 다른 트랜잭션의 동시 접근 원천 차단
   * - Race Condition 방지
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT up FROM UserPerfEntity up "
      + "WHERE up.userId = :userId AND up.gameType = :gameType")
  Optional<UserPerfEntity> findByUserIdAndGameTypeWithLock(
      @Param("userId") Long userId,
      @Param("gameType") GameType gameType
  );

  /**
   * Upsert: User별 GameType의 데이터가 존재하면 Update, 없으면 Insert
   * - Pessimistic Lock을 사용하여 동시성 제어
   * - 중복 데이터 삽입 방지
   * - 트랜잭션 내에서 Lock 획득 후 안전하게 처리
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT up FROM UserPerfEntity up "
      + "WHERE up.userId = :userId AND up.gameType = :gameType")
  Optional<UserPerfEntity> findOrLockForUpsert(
      @Param("userId") Long userId,
      @Param("gameType") GameType gameType
  );

  //GameType별 모든 유효 사용자를 레이팅순으로 조회
  @Query("SELECT up FROM UserPerfEntity up "
      + "WHERE up.gameType = :gameType "
      + "AND up.rated >= 50 "
      + "ORDER BY up.rating DESC")
  List<UserPerfEntity> findRankingByGameType(@Param("gameType") GameType gameType);

  //사용자 순위 계산 (나보다 높은 사람 수 + 1)
  @Query("SELECT COUNT(up) FROM UserPerfEntity up "
      + "WHERE up.gameType = :gameType "
      + "AND up.rated >= 50 "
      + "AND up.rating > :rating")
  int countUsersBetterRating(@Param("gameType") GameType gameType, @Param("rating") int rating);
}


