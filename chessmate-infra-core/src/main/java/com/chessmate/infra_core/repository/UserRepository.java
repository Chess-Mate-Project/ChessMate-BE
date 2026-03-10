package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findById(Long id);
}