package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.LichessUserStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LichessUserStatsRepository extends JpaRepository<LichessUserStats, Long> {
    List<LichessUserStats> findByLichessId(String lichessId);

    Optional<LichessUserStats> findByLichessIdAndGameType(String lichessId, String gameType);

    List<LichessUserStats> findByGameType(String gameType);

    boolean existsByLichessIdAndGameType(String lichessId, String gameType);

    void deleteByLichessIdAndGameType(String lichessId, String gameType);

    List<LichessUserStats> findByRatingBetween(Integer minRating, Integer maxRating);
}

