package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    boolean existsByLichessId(String lichessId);
    
    Optional<UserEntity> findByLichessId(String lichessId);
    
    Optional<UserEntity> findByUsername(String username);
    
    List<UserEntity> findRecentLoginUsersWithin3Days(LocalDateTime threeDaysAgo);
}

