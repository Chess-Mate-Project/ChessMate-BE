package com.chessmate.infra_persistence.lichess.user.repositoryImpl;

import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.infra_persistence.lichess.user.jpaRepository.LichessUserJpaRepository;
import com.chessmate.infra_persistence.lichess.user.mapper.LichessUserMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LichessUserRepositoryImpl implements LichessUserRepository {

  private final LichessUserJpaRepository jpaRepository;
  private final LichessUserMapper mapper;

  @Override
  public boolean existsByLichessId(String lichessId) {
    return jpaRepository.existsByLichessId(lichessId);
  }

  @Override
  public Optional<LichessUser> findByLichessId(String lichessId) {
    return jpaRepository.findByLichessId(lichessId).map(mapper::toDomain);
  }

  @Override
  public Optional<LichessUser> findByUsername(String username) {
    return jpaRepository.findByUsername(username).map(mapper::toDomain);
  }

  @Override
  public Optional<LichessUser> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public LichessUser save(LichessUser user) {
    var entity = mapper.toEntity(user);
    var savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public int count() {
    return (int) jpaRepository.count();
  }

  @Override
  public void updateProfileImage(Long userId, String profileImageUrl) {
    jpaRepository.findById(userId).ifPresentOrElse(
        entity -> {
          entity.setProfileImage(profileImageUrl);
          jpaRepository.save(entity);
        },
        () -> log.warn("[LichessUserRepository] 사용자를 찾을 수 없음: userId={}", userId)
    );
  }

  @Override
  public void updateBannerImage(Long userId, String bannerImageUrl) {
    jpaRepository.findById(userId).ifPresentOrElse(
        entity -> {
          entity.setBannerImage(bannerImageUrl);
          jpaRepository.save(entity);
        },
        () -> log.warn("[LichessUserRepository] 사용자를 찾을 수 없음: userId={}", userId)
    );
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public List<LichessUser> findAll() {
    return jpaRepository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<LichessUser> searchByUsernameContaining(String keyword) {
    log.debug("[LichessUserRepository] username 검색: {}", keyword);
    return jpaRepository.findTop10ByUsernameContainingIgnoreCase(keyword).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}