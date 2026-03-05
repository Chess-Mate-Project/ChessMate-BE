package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.ChesscomUserStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChesscomUserStatsRepository extends JpaRepository<ChesscomUserStatsEntity, Long> {
    List<ChesscomUserStatsEntity> findByPlayerId(Integer playerId);
}

