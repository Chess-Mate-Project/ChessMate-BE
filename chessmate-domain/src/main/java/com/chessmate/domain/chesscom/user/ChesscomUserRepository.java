package com.chessmate.domain.chesscom.user;

import java.util.Optional;

/**
 * ChesscomUser 도메인 리포지토리 인터페이스
 * Chess.com 사용자 계정 정보 관리
 */
public interface ChesscomUserRepository {

  /**
   * 사용자 ID로 ChesscomUser 조회
   * @param id 사용자 ID
   * @return Optional<ChesscomUser> 조회된 사용자
   */
  Optional<ChesscomUser> findById(Long id);

  /**
   * Chess.com ID로 ChesscomUser 조회
   * @param chesscomId Chess.com ID
   * @return Optional<ChesscomUser> 조회된 사용자
   */
  Optional<ChesscomUser> findByChesscomId(Long chesscomId);

  /**
   * 사용자명으로 ChesscomUser 조회
   * @param username Chess.com 사용자명
   * @return Optional<ChesscomUser> 조회된 사용자
   */
  Optional<ChesscomUser> findByUsername(String username);

  /**
   * ChesscomUser 저장
   * @param user 저장할 사용자
   * @return ChesscomUser 저장된 사용자
   */
  ChesscomUser save(ChesscomUser user);

  /**
   * 사용자 ID로 ChesscomUser 삭제
   * @param id 사용자 ID
   */
  void deleteById(Long id);
}
