package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.infra_persistence.entity.UserDailyStreakEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDailyStreakJpaRepository extends
    JpaRepository<UserDailyStreakEntity, Long> {

}
