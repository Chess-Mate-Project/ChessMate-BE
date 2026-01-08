package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.infra_persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository
    extends JpaRepository<UserEntity, Long> {

  boolean existsByLichessId(String lichessId);
  Optional<UserEntity> findById(Long id);
  Optional<UserEntity> findByLichessId(String lichessId);
  Optional<UserEntity> findByUsername(String username);
}