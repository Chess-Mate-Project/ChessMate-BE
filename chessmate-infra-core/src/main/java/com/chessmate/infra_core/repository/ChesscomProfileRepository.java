package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.ChesscomProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChesscomProfileRepository extends JpaRepository<ChesscomProfile, Long> {
    Optional<ChesscomProfile> findByChesscomId(Integer ChesscomId);

    Optional<ChesscomProfile> findByUserId(Long userId);

    Optional<ChesscomProfile> findByUsername(String username);

    boolean existsByChesscomId(Integer ChesscomId);


    void deleteByChesscomId(Integer ChesscomId);
}

