package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.domain.userColorStat.UserColorStatRepository;
import com.chessmate.infra_persistence.entity.UserColorStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserColorStatJpaRepository extends
    JpaRepository<UserColorStatEntity, Long> {

}
