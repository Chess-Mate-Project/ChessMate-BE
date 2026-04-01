//package com.chessmate.infra_persistence.jpaRepository;
//
//import com.chessmate.common.type.ChessColor;
//import com.chessmate.common.type.GameResult;
//import com.chessmate.common.type.GameType;
//import com.chessmate.infra_persistence.entity.LichessUserColorStatEntity;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//import java.util.Optional;
//
///**
// * Lichess 사용자 색상별 통계 JPA Repository
// * - LichessUserColorStatEntity에 대한 데이터베이스 접근을 담당
// */
//public interface LichessUserColorStatJpaRepository extends JpaRepository<LichessUserColorStatEntity, Long> {
//
//  /**
//   * 사용자의 모든 색상별 통계 조회
//   * @param userId 사용자 ID
//   * @return 통계 목록
//   */
//  List<LichessUserColorStatEntity> findByUserId(Long userId);
//
//  /**
//   * 사용자의 특정 게임 종목 색상별 통계 조회
//   * @param userId 사용자 ID
//   * @param gameType 게임 종목
//   * @return 통계 목록
//   */
//  List<LichessUserColorStatEntity> findByUserIdAndGameType(Long userId, GameType gameType);
//
//  /**
//   * 사용자의 특정 색상에서의 모든 결과 통계 조회
//   * @param userId 사용자 ID
//   * @param color 체스 색상
//   * @return 통계 목록
//   */
//  List<LichessUserColorStatEntity> findByUserIdAndColor(Long userId, ChessColor color);
//
//  /**
//   * 사용자의 색상별 결과 통계 조회
//   * @param userId 사용자 ID
//   * @param color 체스 색상 (WHITE/BLACK)
//   * @param result 게임 결과 (WIN/LOSS/DRAW)
//   * @param gameType 게임 종목
//   * @return Optional<LichessUserColorStatEntity>
//   */
//  @Query("SELECT s FROM LichessUserColorStatEntity s WHERE s.userId = :userId AND s.color = :color AND s.result = :result AND s.gameType = :gameType")
//  Optional<LichessUserColorStatEntity> findByUserIdAndColorAndResultAndGameType(
//      @Param("userId") Long userId,
//      @Param("color") ChessColor color,
//      @Param("result") GameResult result,
//      @Param("gameType") GameType gameType
//  );
//
//  /**
//   * 특정 색상 결과의 사용자들 조회
//   * @param color 체스 색상
//   * @param result 게임 결과
//   * @return 통계 목록
//   */
//  @Query("SELECT s FROM LichessUserColorStatEntity s WHERE s.color = :color AND s.result = :result")
//  List<LichessUserColorStatEntity> findByColorAndResult(
//      @Param("color") ChessColor color,
//      @Param("result") GameResult result
//  );
//}
