package com.chessmate.infra_persistence.stat.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserDailyGameStatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDailyGameStatJpaRepository extends JpaRepository<UserDailyGameStatJpaEntity, Long> {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserDailyGameStatJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    @Query("SELECT s FROM UserDailyGameStatJpaEntity s WHERE s.userId = :userId AND s.platform = :platform AND YEAR(s.date) = :year")
    List<UserDailyGameStatJpaEntity> findByUserIdAndPlatformAndYear(
        @Param("userId") Long userId,
        @Param("platform") OAuthPlatForm platform,
        @Param("year") int year
    );
}