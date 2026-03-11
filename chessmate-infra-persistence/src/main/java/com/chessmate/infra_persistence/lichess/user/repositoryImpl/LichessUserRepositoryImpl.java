package com.chessmate.infra_persistence.lichess.user.repositoryImpl;

import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.infra_persistence.lichess.user.jpaRepository.LichessUserJpaRepository;
import com.chessmate.infra_persistence.lichess.user.mapper.LichessUserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Lichess 사용자 Repository 구현체
 * - 도메인 인터페이스를 JPA를 통해 구현
 * - 데이터베이스 접근 로직을 캡슐화
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class LichessUserRepositoryImpl implements LichessUserRepository {

  private final LichessUserJpaRepository jpaRepository;
  private final LichessUserMapper mapper;

  /**
   * Lichess ID로 사용자 존재 여부 확인
   *
   * @param lichessId Lichess 사용자 ID
   * @return 존재 여부 (true: 존재, false: 미존재)
   */
  @Override
  public boolean existsByLichessId(String lichessId) {
    log.debug("[LichessUserRepository] Lichess ID 존재 여부 확인: {}", lichessId);
    return jpaRepository.existsByLichessId(lichessId);
  }

  /**
   * Lichess ID로 사용자 조회
   *
   * @param lichessId Lichess 사용자 ID
   * @return Optional<LichessUser> 사용자 도메인 객체
   */
  @Override
  public Optional<LichessUser> findByLichessId(String lichessId) {
    log.debug("[LichessUserRepository] Lichess ID로 사용자 조회: {}", lichessId);
    return jpaRepository.findByLichessId(lichessId)
        .map(mapper::toDomain);
  }

  /**
   * 사용자명으로 사용자 조회
   *
   * @param username 사용자명
   * @return Optional<LichessUser> 사용자 도메인 객체
   */
  @Override
  public Optional<LichessUser> findByUsername(String username) {
    log.debug("[LichessUserRepository] 사용자명으로 조회: {}", username);
    return jpaRepository.findByUsername(username)
        .map(mapper::toDomain);
  }

  /**
   * ID로 사용자 조회
   *
   * @param id 사용자 ID (Primary Key)
   * @return Optional<LichessUser> 사용자 도메인 객체
   */
  @Override
  public Optional<LichessUser> findById(Long id) {
    log.debug("[LichessUserRepository] ID로 사용자 조회: {}", id);
    return jpaRepository.findById(id)
        .map(mapper::toDomain);
  }

  /**
   * 사용자 저장 또는 업데이트
   *
   * @param user 저장할 사용자 도메인 객체
   * @return 저장된 사용자 도메인 객체 (ID가 설정됨)
   */
  @Override
  public LichessUser save(LichessUser user) {
    log.debug("[LichessUserRepository] 사용자 저장: {}", user.getLichessId());
    var entity = mapper.toEntity(user);
    var savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  /**
   * 전체 사용자 수 조회
   *
   * @return 총 사용자 수
   */
  @Override
  public int count() {
    log.debug("[LichessUserRepository] 전체 사용자 수 조회");
    return (int) jpaRepository.count();
  }

  /**
   * 프로필 이미지 URL 업데이트
   *
   * @param userId 사용자 ID
   * @param profileImageUrl 프로필 이미지 URL
   */
  @Override
  public void updateProfileImage(Long userId, String profileImageUrl) {
    log.debug("[LichessUserRepository] 프로필 이미지 업데이트: userId={}", userId);
    jpaRepository.findById(userId)
        .ifPresentOrElse(
            entity -> {
              entity.setProfileImage(profileImageUrl);
              jpaRepository.save(entity);
              log.info("[LichessUserRepository] 프로필 이미지 업데이트 완료: userId={}", userId);
            },
            () -> log.warn("[LichessUserRepository] 사용자를 찾을 수 없음: userId={}", userId)
        );
  }

  /**
   * 배너 이미지 URL 업데이트
   *
   * @param userId 사용자 ID
   * @param bannerImageUrl 배너 이미지 URL
   */
  @Override
  public void updateBannerImage(Long userId, String bannerImageUrl) {
    log.debug("[LichessUserRepository] 배너 이미지 업데이트: userId={}", userId);
    jpaRepository.findById(userId)
        .ifPresentOrElse(
            entity -> {
              entity.setBannerImage(bannerImageUrl);
              jpaRepository.save(entity);
              log.info("[LichessUserRepository] 배너 이미지 업데이트 완료: userId={}", userId);
            },
            () -> log.warn("[LichessUserRepository] 사용자를 찾을 수 없음: userId={}", userId)
        );
  }

  /**
   * 최근 3일 이내 로그인한 사용자 조회
   *
   * @return 지난 3일 이내 로그인 기록이 있는 사용자 목록
   */
  @Override
  public List<LichessUser> findRecentLoginUsersWithin3Days() {
    log.debug("[LichessUserRepository] 최근 3일 이내 로그인 사용자 조회");
    LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
    return jpaRepository.findRecentLoginUsersWithin3Days(threeDaysAgo)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  /**
   * ID로 사용자 삭제
   *
   * @param id 삭제할 사용자 ID
   */
  @Override
  public void deleteById(Long id) {
    log.debug("[LichessUserRepository] 사용자 삭제: id={}", id);
    jpaRepository.deleteById(id);
  }
}

