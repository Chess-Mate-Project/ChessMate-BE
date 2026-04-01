package com.chessmate.infra_persistence.chesscom.user.jpaRepository;

import com.chessmate.infra_persistence.chesscom.user.entity.ChesscomUserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Chess.com 사용자 JPA Repository
 * - ChesscomUserEntity에 대한 데이터베이스 접근을 담당
 */
@Repository
public interface ChesscomUserJpaRepository extends JpaRepository<ChesscomUserEntity, Long> {

  /**
   * Chess.com ID로 사용자 조회
   * @param chesscomId Chess.com 사용자 ID
   * @return 사용자 정보
   */
  Optional<ChesscomUserEntity> findByChesscomId(Long chesscomId);

  /**
   * 사용자명으로 사용자 조회
   * @param username Chess.com 사용자명
   * @return 사용자 정보
   */
  Optional<ChesscomUserEntity> findByUsername(String username);
}

