package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.ChesscomStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChesscomStreakRepository extends JpaRepository<ChesscomStreak, Long> {
    Optional<ChesscomStreak> findByPlayerIdAndDate(Integer playerId, LocalDate date);

    List<ChesscomStreak> findByPlayerIdOrderByDateDesc(Integer playerId);
}

