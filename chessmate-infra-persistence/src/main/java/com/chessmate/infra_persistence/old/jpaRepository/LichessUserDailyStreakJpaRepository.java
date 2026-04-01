//package com.chessmate.infra_persistence.jpaRepository;
//
//import com.chessmate.infra_persistence.entity.LichessUserDailyStreakEntity;
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Optional;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
///**
// * Lichess 사용자 일일 스트릭 JPA Repository
// * - LichessUserDailyStreakEntity에 대한 데이터베이스 접근을 담당
// */
//public interface LichessUserDailyStreakJpaRepository extends JpaRepository<LichessUserDailyStreakEntity, Long> {
//
//  /**
//   * 사용자의 특정 날짜 스트릭 조회
//   * @param userId 사용자 ID
//   * @param date 조회할 날짜
//   * @return Optional<LichessUserDailyStreakEntity>
//   */
//  Optional<LichessUserDailyStreakEntity> findByUserIdAndDate(Long userId, LocalDate date);
//
//  /**
//   * 사용자의 최근 스트릭들 조회
//   * @param userId 사용자 ID
//   * @param startDate 조회할 날짜 범위
//   * @return 스트릭 목록
//   */
//  @Query("SELECT s FROM LichessUserDailyStreakEntity s WHERE s.userId = :userId AND s.date >= :startDate ORDER BY s.date DESC")
//  List<LichessUserDailyStreakEntity> findRecentStreaks(
//      @Param("userId") Long userId,
//      @Param("startDate") LocalDate startDate
//  );
//
//  /**
//   * 사용자의 특정 기간 스트릭 조회
//   * @param userId 사용자 ID
//   * @param startDate 시작 날짜
//   * @param endDate 종료 날짜
//   * @return 스트릭 목록
//   */
//  @Query("SELECT s FROM LichessUserDailyStreakEntity s WHERE s.userId = :userId AND s.date BETWEEN :startDate AND :endDate ORDER BY s.date DESC")
//  List<LichessUserDailyStreakEntity> findStreaksBetweenDates(
//      @Param("userId") Long userId,
//      @Param("startDate") LocalDate startDate,
//      @Param("endDate") LocalDate endDate
//  );
//}
//
