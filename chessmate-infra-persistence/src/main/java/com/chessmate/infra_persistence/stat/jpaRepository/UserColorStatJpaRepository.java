package com.chessmate.infra_persistence.stat.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserColorStatJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserColorStatJpaRepository extends JpaRepository<UserColorStatJpaEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserColorStatJpaEntity s WHERE s.userId = :userId AND s.platform = :platform")
    void deleteByUserIdAndPlatform(@Param("userId") Long userId, @Param("platform") OAuthPlatForm platform);
    List<UserColorStatJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    List<UserColorStatJpaEntity> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);

    boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}