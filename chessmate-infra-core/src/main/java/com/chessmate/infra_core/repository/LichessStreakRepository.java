package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.LichessStreakEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LichessStreakRepository extends JpaRepository<LichessStreakEntity, Long> {
    Optional<LichessStreakEntity> findByLichessIdAndDate(String lichessId, LocalDate date);

    List<LichessStreakEntity> findByLichessIdOrderByDateDesc(String lichessId);
}

