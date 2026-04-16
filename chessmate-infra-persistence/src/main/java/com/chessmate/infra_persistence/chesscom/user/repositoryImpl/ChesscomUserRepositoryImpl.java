package com.chessmate.infra_persistence.chesscom.user.repositoryImpl;

import com.chessmate.domain.chesscom.user.ChesscomUser;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.infra_persistence.chesscom.user.jpaRepository.ChesscomUserJpaRepository;
import com.chessmate.infra_persistence.chesscom.user.mapper.ChesscomUserMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Chess.com 사용자 Repository 구현체
 * - 도메인 인터페이스를 JPA를 통해 구현
 * - 데이터베이스 접근 로직을 캡슐화
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChesscomUserRepositoryImpl implements ChesscomUserRepository {

  private final ChesscomUserJpaRepository jpaRepository;
  private final ChesscomUserMapper mapper;

  /**
   * Chess.com ID로 사용자 존재 여부 확인
   *
   * @param chesscomId Chess.com 사용자 ID
   * @return 존재 여부 (true: 존재, false: 미존재)
   */
  @Override
  public boolean existsByChesscomId(Long chesscomId) {
    log.debug("[ChesscomUserRepository] Chess.com ID 존재 여부 확인: {}", chesscomId);
    return jpaRepository.findByChesscomId(chesscomId).isPresent();
  }

  /**
   * Chess.com ID로 사용자 조회
   *
   * @param chesscomId Chess.com 사용자 ID
   * @return Optional<ChesscomUser> 사용자 도메인 객체
   */
  @Override
  public Optional<ChesscomUser> findByChesscomId(Long chesscomId) {
    log.debug("[ChesscomUserRepository] Chess.com ID로 사용자 조회: {}", chesscomId);
    return jpaRepository.findByChesscomId(chesscomId)
        .map(mapper::toDomain);
  }

  /**
   * 사용자명으로 사용자 조회
   *
   * @param username 사용자명
   * @return Optional<ChesscomUser> 사용자 도메인 객체
   */
  @Override
  public Optional<ChesscomUser> findByUsername(String username) {
    log.debug("[ChesscomUserRepository] 사용자명으로 조회: {}", username);
    return jpaRepository.findByUsername(username)
        .map(mapper::toDomain);
  }

  /**
   * ID로 사용자 조회
   *
   * @param id 사용자 ID (Primary Key)
   * @return Optional<ChesscomUser> 사용자 도메인 객체
   */
  @Override
  public Optional<ChesscomUser> findById(Long id) {
    log.debug("[ChesscomUserRepository] ID로 사용자 조회: {}", id);
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
  public ChesscomUser save(ChesscomUser user) {
    log.debug("[ChesscomUserRepository] 사용자 저장: {}", user.getChesscomId());
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
    log.debug("[ChesscomUserRepository] 전체 사용자 수 조회");
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
    log.debug("[ChesscomUserRepository] 프로필 이미지 업데이트: userId={}, profileImageUrl={}", userId, profileImageUrl);
    jpaRepository.findById(userId).ifPresent(entity -> {
      entity.setProfile(profileImageUrl);
      jpaRepository.save(entity);
    });
  }

  /**
   * 배너 이미지 URL 업데이트
   *
   * @param userId 사용자 ID
   * @param bannerImageUrl 배너 이미지 URL
   */
  @Override
  public void updateBannerImage(Long userId, String bannerImageUrl) {
    log.debug("[ChesscomUserRepository] 배너 이미지 업데이트: userId={}, bannerImageUrl={}", userId, bannerImageUrl);
    jpaRepository.findById(userId).ifPresent(entity -> {
      entity.setBanner(bannerImageUrl);
      jpaRepository.save(entity);
    });
  }

  @Override
  public List<ChesscomUser> findAll() {
    return jpaRepository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<ChesscomUser> searchByUsernameContaining(String keyword) {
    log.debug("[ChesscomUserRepository] username 검색: {}", keyword);
    return jpaRepository.findTop10ByUsernameContainingIgnoreCase(keyword).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}

