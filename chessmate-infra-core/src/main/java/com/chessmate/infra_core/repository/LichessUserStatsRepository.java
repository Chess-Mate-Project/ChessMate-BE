package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.LichessUserStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LichessUserStatsRepository extends JpaRepository<LichessUserStatsEntity, Long> {
    List<LichessUserStatsEntity> findByLichessId(String lichessId);

    Optional<LichessUserStatsEntity> findByLichessIdAndGameType(String lichessId, String gameType);

    List<LichessUserStatsEntity> findByGameType(String gameType);

    boolean existsByLichessIdAndGameType(String lichessId, String gameType);

    void deleteByLichessIdAndGameType(String lichessId, String gameType);

    List<LichessUserStatsEntity> findByRatingBetween(Integer minRating, Integer maxRating);
}

