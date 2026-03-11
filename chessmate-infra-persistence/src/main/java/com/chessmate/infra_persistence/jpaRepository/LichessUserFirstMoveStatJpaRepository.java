//package com.chessmate.infra_persistence.jpaRepository;
//
//import com.chessmate.common.type.ChessColor;
//import com.chessmate.common.type.GameType;
//import com.chessmate.infra_persistence.entity.LichessUserFirstMoveStatEntity;
//import java.util.List;
//import java.util.Optional;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
///**
// * Lichess 사용자 첫 수 통계 JPA Repository
// * - LichessUserFirstMoveStatEntity에 대한 데이터베이스 접근을 담당
// */
//public interface LichessUserFirstMoveStatJpaRepository extends JpaRepository<LichessUserFirstMoveStatEntity, Long> {
//
//  /**
//   * 사용자의 특정 색상 첫 수 통계 조회
//   * @param userId 사용자 ID
//   * @param color 체스 색상 (WHITE/BLACK)
//   * @param gameType 게임 종목
//   * @return 첫 수 통계 목록
//   */
//  List<LichessUserFirstMoveStatEntity> findByUserIdAndColorAndGameType(
//      Long userId, ChessColor color, GameType gameType
//  );
//
//  /**
//   * 사용자의 특정 첫 수 통계 조회
//   * @param userId 사용자 ID
//   * @param firstMove 첫 수 (e.g., "e2e4")
//   * @param color 체스 색상
//   * @return Optional<LichessUserFirstMoveStatEntity>
//   */
//  Optional<LichessUserFirstMoveStatEntity> findByUserIdAndFirstMoveAndColor(
//      Long userId, String firstMove, ChessColor color
//  );
//
//  /**
//   * 사용자의 모든 첫 수 통계 조회
//   * @param userId 사용자 ID
//   * @return 첫 수 통계 목록
//   */
//  List<LichessUserFirstMoveStatEntity> findByUserId(Long userId);
//
//  /**
//   * 사용자의 특정 게임 종목 첫 수 통계 조회
//   * @param userId 사용자 ID
//   * @param gameType 게임 종목
//   * @return 첫 수 통계 목록
//   */
//  List<LichessUserFirstMoveStatEntity> findByUserIdAndGameType(Long userId, GameType gameType);
//
//  /**
//   * 특정 첫 수를 사용하는 사용자들 조회
//   * @param firstMove 첫 수
//   * @param gameType 게임 종목
//   * @return 통계 목록
//   */
//  @Query("SELECT f FROM LichessUserFirstMoveStatEntity f WHERE f.firstMove = :firstMove AND f.gameType = :gameType ORDER BY f.id DESC LIMIT 10")
//  List<LichessUserFirstMoveStatEntity> findTopByFirstMoveAndGameType(
//      @Param("firstMove") String firstMove,
//      @Param("gameType") GameType gameType
//  );
//}
//
