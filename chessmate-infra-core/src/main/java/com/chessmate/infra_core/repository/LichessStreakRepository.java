package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.LichessStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LichessStreakRepository extends JpaRepository<LichessStreak, Long> {
    Optional<LichessStreak> findByLichessIdAndDate(String lichessId, LocalDate date);

    List<LichessStreak> findByLichessIdOrderByDateDesc(String lichessId);
}

