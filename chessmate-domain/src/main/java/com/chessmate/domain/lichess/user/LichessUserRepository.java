package com.chessmate.domain.lichess.user;

import java.util.List;
import java.util.Optional;

/**
 * Lichess 사용자 도메인 Repository 인터페이스
 * - 사용자 데이터 접근 계약 정의
 * - 구현체는 infra_persistence에서 제공
 */
public interface LichessUserRepository {

  /**
   * Lichess ID로 사용자 존재 여부 확인
   *
   * @param lichessId Lichess 사용자 ID
   * @return 존재 여부 (true: 존재, false: 미존재)
   */
  boolean existsByLichessId(String lichessId);

  /**
   * Lichess ID로 사용자 조회
   *
   * @param lichessId Lichess 사용자 ID
   * @return Optional<LichessUser> 사용자 도메인 객체
   */
  Optional<LichessUser> findByLichessId(String lichessId);

  /**
   * 사용자명으로 사용자 조회
   *
   * @param username 사용자명
   * @return Optional<LichessUser> 사용자 도메인 객체
   */
  Optional<LichessUser> findByUsername(String username);

  /**
   * ID로 사용자 조회
   *
   * @param id 사용자 ID (Primary Key)
   * @return Optional<LichessUser> 사용자 도메인 객체
   */
  Optional<LichessUser> findById(Long id);

  /**
   * 사용자 저장 또는 업데이트
   *
   * @param user 저장할 사용자 도메인 객체
   * @return 저장된 사용자 도메인 객체 (ID가 설정됨)
   */
  LichessUser save(LichessUser user);

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
   * 최근 3일 이내 로그인한 사용자 조회
   *
   * @return 지난 3일 이내 로그인 기록이 있는 사용자 목록
   */
  List<LichessUser> findRecentLoginUsersWithin3Days();

  /**
   * ID로 사용자 삭제
   *
   * @param id 삭제할 사용자 ID
   */
  void deleteById(Long id);
}
