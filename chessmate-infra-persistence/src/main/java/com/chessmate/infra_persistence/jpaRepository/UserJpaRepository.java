package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.infra_persistence.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository
    extends JpaRepository<UserEntity, Long> {

  boolean existsByLichessId(String lichessId);
  Optional<UserEntity> findById(Long id);
  Optional<UserEntity> findByLichessId(String lichessId);
  Optional<UserEntity> findByUsername(String username);

  @Query("SELECT u FROM UserEntity u WHERE u.lastLoginAt >= :threeDaysAgo ORDER BY u.lastLoginAt DESC")
  List<UserEntity> findRecentLoginUsersWithin3Days(@Param("threeDaysAgo") LocalDateTime threeDaysAgo);
}