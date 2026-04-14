package com.chessmate.domain.chesscom.user;

import java.util.List;
import java.util.Optional;

/**
 * Chess.com 사용자 도메인 Repository 인터페이스
 * - 사용자 데이터 접근 계약 정의
 * - 구현체는 infra_persistence에서 제공
 */
public interface ChesscomUserRepository {

  /**
   * Chess.com ID로 사용자 존재 여부 확인
   *
   * @param chesscomId Chess.com 사용자 ID
   * @return 존재 여부 (true: 존재, false: 미존재)
   */
  boolean existsByChesscomId(Long chesscomId);

  /**
   * Chess.com ID로 사용자 조회
   *
   * @param chesscomId Chess.com 사용자 ID
   * @return Optional<ChesscomUser> 사용자 도메인 객체
   */
  Optional<ChesscomUser> findByChesscomId(Long chesscomId);

  /**
   * 사용자명으로 사용자 조회
   *
   * @param username 사용자명
   * @return Optional<ChesscomUser> 사용자 도메인 객체
   */
  Optional<ChesscomUser> findByUsername(String username);

  /**
   * ID로 사용자 조회
   *
   * @param id 사용자 ID (Primary Key)
   * @return Optional<ChesscomUser> 사용자 도메인 객체
   */
  Optional<ChesscomUser> findById(Long id);

  /**
   * 사용자 저장 또는 업데이트
   *
   * @param user 저장할 사용자 도메인 객체
   * @return 저장된 사용자 도메인 객체 (ID가 설정됨)
   */
  ChesscomUser save(ChesscomUser user);

  /**
   * 전체 사용자 수 조회
   *
   * @return 총 사용자 수
   */
  int count();

  /**
   * 프로필 이미지 URL 업데이트
   *
   * @param userId 사용자 ID
   * @param profileImageUrl 프로필 이미지 URL
   */
  void updateProfileImage(Long userId, String profileImageUrl);

  /**
   * 배너 이미지 URL 업데이트
   *
   * @param userId 사용자 ID
   * @param bannerImageUrl 배너 이미지 URL
   */
  void updateBannerImage(Long userId, String bannerImageUrl);

  /**
   * 전체 Chess.com 사용자 목록 조회 (정기 증분 수집 스케쥴러용)
   */
  List<ChesscomUser> findAll();
}

