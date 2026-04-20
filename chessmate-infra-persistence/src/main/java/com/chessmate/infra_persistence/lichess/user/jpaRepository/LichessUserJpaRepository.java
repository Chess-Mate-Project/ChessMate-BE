package com.chessmate.infra_persistence.lichess.user.jpaRepository;

import com.chessmate.infra_persistence.lichess.user.entity.LichessUserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichessUserJpaRepository extends JpaRepository<LichessUserEntity, Long> {

  boolean existsByLichessId(String lichessId);

  Optional<LichessUserEntity> findByLichessId(String lichessId);

  Optional<LichessUserEntity> findByUsername(String username);

  long countByDeletedAtIsNull();

  List<LichessUserEntity> findAllByDeletedAtIsNull();

  List<LichessUserEntity> findTop10ByUsernameContainingIgnoreCaseAndDeletedAtIsNull(String keyword);
}
