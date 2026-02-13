package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.common.type.GameType;
import com.chessmate.infra_persistence.entity.UserPerfEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPerfJpaRepository extends JpaRepository<UserPerfEntity, Long> {
  Optional<UserPerfEntity> findByUserIdAndGameType(Long userId, GameType gameType);

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

