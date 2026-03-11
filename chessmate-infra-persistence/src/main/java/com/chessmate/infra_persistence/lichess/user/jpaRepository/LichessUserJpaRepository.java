package com.chessmate.infra_persistence.lichess.user.jpaRepository;

import com.chessmate.infra_persistence.lichess.user.entity.LichessUserEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Lichess 사용자 JPA Repository
 * - LichessUserEntity에 대한 데이터베이스 접근을 담당
 */
public interface LichessUserJpaRepository extends JpaRepository<LichessUserEntity, Long> {

  /**
   * Lichess ID로 사용자 존재 여부 확인
   * @param lichessId Lichess 사용자 ID
   * @return 존재 여부
   */
  boolean existsByLichessId(String lichessId);

  /**
   * Lichess ID로 사용자 조회
   * @param lichessId Lichess 사용자 ID
   * @return Optional<LichessUserEntity>
   */
  Optional<LichessUserEntity> findByLichessId(String lichessId);

  /**
   * 사용자명으로 사용자 조회
   * @param username 사용자명
   * @return Optional<LichessUserEntity>
   */
  Optional<LichessUserEntity> findByUsername(String username);

  /**
   * 최근 3일 이내 로그인한 사용자 조회
   * @param threeDaysAgo 3일 전 날짜/시간
   * @return 최근 로그인 사용자 목록
   */
  @Query("SELECT u FROM LichessUserEntity u WHERE u.lastLoginAt >= :threeDaysAgo ORDER BY u.lastLoginAt DESC")
  List<LichessUserEntity> findRecentLoginUsersWithin3Days(@Param("threeDaysAgo") LocalDateTime threeDaysAgo);
}

