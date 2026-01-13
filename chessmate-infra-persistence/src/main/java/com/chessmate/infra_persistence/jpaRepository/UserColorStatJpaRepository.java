package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.common.type.GameType;
import com.chessmate.domain.userColorStat.UserColorStatRepository;
import com.chessmate.infra_persistence.entity.UserColorStatEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserColorStatJpaRepository extends
    JpaRepository<UserColorStatEntity, Long> {

  List<UserColorStatEntity> findByUserIdAndGameType(Long userId, GameType gameType);
}
