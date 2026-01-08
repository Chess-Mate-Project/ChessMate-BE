package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.infra_persistence.entity.UserFirstMoveStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFirstMoveStatJpaRepository extends
    JpaRepository<UserFirstMoveStatEntity, Long> {

}
