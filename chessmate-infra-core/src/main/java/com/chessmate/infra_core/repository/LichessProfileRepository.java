package com.chessmate.infra_core.repository;

import com.chessmate.infra_core.entity.LichessProfileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichessProfileRepository extends JpaRepository<LichessProfileEntity, String> {

    Optional<LichessProfileEntity> findByUserId(Long userId);

    Optional<LichessProfileEntity> findByLichessId(String lichessId);

    boolean existsByUserId(Long userId);

    boolean existsByLichessId(String lichessId);

    void deleteByLichessId(String lichessId);
}


