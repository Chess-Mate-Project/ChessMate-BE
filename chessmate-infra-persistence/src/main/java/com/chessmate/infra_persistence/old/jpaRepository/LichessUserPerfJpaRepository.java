//package com.chessmate.infra_persistence.jpaRepository;
//
//import com.chessmate.common.type.GameType;
//import com.chessmate.infra_persistence.entity.LichessUserPerfEntity;
//import java.util.List;
//import java.util.Optional;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
///**
// * Lichess 사용자 성능 JPA Repository
// * - LichessUserPerfEntity에 대한 데이터베이스 접근을 담당
// */
//public interface LichessUserPerfJpaRepository extends JpaRepository<LichessUserPerfEntity, Long> {
//
//  /**
//   * 사용자의 특정 게임 종목 성능 조회
//   * @param userId 사용자 ID
//   * @param gameType 게임 종목
//   * @return Optional<LichessUserPerfEntity>
//   */
//  Optional<LichessUserPerfEntity> findByUserIdAndGameType(Long userId, GameType gameType);
//
//  /**
//   * 사용자의 모든 게임 종목 성능 조회
//   * @param userId 사용자 ID
//   * @return 성능 목록
//   */
//  List<LichessUserPerfEntity> findByUserId(Long userId);
//
//  /**
//   * 특정 레이팅 이상의 사용자들 조회
//   * @param gameType 게임 종목
//   * @param minRating 최소 레이팅
//   * @return 사용자 성능 목록
//   */
//  @Query("SELECT p FROM LichessUserPerfEntity p WHERE p.gameType = :gameType AND p.rating >= :minRating ORDER BY p.rating DESC")
//  List<LichessUserPerfEntity> findTop10ByGameTypeAndRating(
//      @Param("gameType") GameType gameType,
//      @Param("minRating") int minRating
//  );
//}
//
