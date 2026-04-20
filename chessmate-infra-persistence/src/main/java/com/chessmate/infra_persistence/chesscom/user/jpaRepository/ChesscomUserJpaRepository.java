package com.chessmate.infra_persistence.chesscom.user.jpaRepository;

import com.chessmate.infra_persistence.chesscom.user.entity.ChesscomUserEntity;
import java.util.List;
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

  /**
   * username에 keyword가 포함된 사용자 목록 조회
   * @param keyword 검색 키워드
   * @return 매칭된 사용자 엔티티 목록
   */
  List<ChesscomUserEntity> findAllByDeletedAtIsNull();

  List<ChesscomUserEntity> findTop10ByUsernameContainingIgnoreCaseAndDeletedAtIsNull(String keyword);
}

