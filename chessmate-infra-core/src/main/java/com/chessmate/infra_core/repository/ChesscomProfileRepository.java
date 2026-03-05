package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.ChesscomProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChesscomProfileRepository extends JpaRepository<ChesscomProfileEntity, Long> {
    Optional<ChesscomProfileEntity> findByPlayerId(Integer playerId);

    Optional<ChesscomProfileEntity> findByUserId(Long userId);

    Optional<ChesscomProfileEntity> findByUsername(String username);

    boolean existsByPlayerId(Integer playerId);

    boolean existsByUserId(Long userId);

    void deleteByPlayerId(Integer playerId);
}

