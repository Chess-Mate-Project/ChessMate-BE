package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.domain.user.User;
import com.chessmate.domain.user.UserRepository;
import com.chessmate.infra_persistence.entity.UserEntity;
import com.chessmate.infra_persistence.jpaRepository.UserJpaRepository;
import com.chessmate.infra_persistence.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

  private final UserJpaRepository jpaRepository;

  @Override
  public boolean existsByLichessId(String lichessId) {
    return jpaRepository.existsByLichessId(lichessId);
  }

  @Override
  public Optional<User> findByLichessId(String lichessId) {
    return jpaRepository.findByLichessId(lichessId)
        .map(UserMapper::toDomain);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return jpaRepository.findByUsername(username)
        .map(UserMapper::toDomain);
  }

  @Override
  public Optional<User> findById(Long id) {
    return jpaRepository.findById(id)
        .map(UserMapper::toDomain);
  }

  @Override
  public User save(User user) {
    UserEntity entity = UserMapper.toEntity(user);
    UserEntity saved = jpaRepository.save(entity);
    return UserMapper.toDomain(saved);
  }

  @Override
  public int count() {
    return Math.toIntExact(jpaRepository.count());
  }

  @Override
  public void updateProfileImage(Long userId, String profileImageUrl) {
    UserEntity user = jpaRepository.findById(userId)
        .orElseThrow();

    user.setProfileImage(profileImageUrl);

    jpaRepository.save(user);
  }

  @Override
  public void updateBannerImage(Long userId, String bannerImageUrl) {
    UserEntity user = jpaRepository.findById(userId)
        .orElseThrow();

    user.setBannerImage(bannerImageUrl);
    jpaRepository.saveAndFlush(user);
  }

  @Override
  public List<User> findRecentLoginUsersWithin3Days() {
    // 현재 시간으로부터 3일 전의 시간 계산
    LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

    // JpaRepository 메서드 호출
    List<UserEntity> entities = jpaRepository.findRecentLoginUsersWithin3Days(threeDaysAgo);

    // 엔티티를 도메인으로 변환하여 반환
    return entities.stream()
        .map(UserMapper::toDomain)
        .toList();
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
  }
}
