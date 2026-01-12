package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.domain.user.User;
import com.chessmate.domain.user.UserRepository;
import com.chessmate.infra_persistence.entity.UserEntity;
import com.chessmate.infra_persistence.jpaRepository.UserJpaRepository;
import com.chessmate.infra_persistence.mapper.UserMapper;
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
  }

  @Override
  public void updateBannerImage(Long userId, String bannerImageUrl) {
    UserEntity user = jpaRepository.findById(userId)
        .orElseThrow();

    user.setBannerImage(bannerImageUrl);
  }
}
