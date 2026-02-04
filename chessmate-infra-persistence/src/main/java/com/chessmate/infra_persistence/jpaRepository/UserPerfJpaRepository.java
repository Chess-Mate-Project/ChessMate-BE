package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.common.type.GameType;
import com.chessmate.infra_persistence.entity.UserPerfEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPerfJpaRepository extends JpaRepository<UserPerfEntity, Long> {
  Optional<UserPerfEntity> findByUserIdAndGameType(Long userId, GameType gameType);
}

