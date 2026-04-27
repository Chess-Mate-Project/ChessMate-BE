package com.chessmate.infra_persistence.stat.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserMonthlyRatingStatJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMonthlyRatingStatJpaRepository extends JpaRepository<UserMonthlyRatingStatJpaEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserMonthlyRatingStatJpaEntity s WHERE s.userId = :userId AND s.platform = :platform")
    void deleteByUserIdAndPlatform(@Param("userId") Long userId, @Param("platform") OAuthPlatForm platform);

    List<UserMonthlyRatingStatJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    Optional<UserMonthlyRatingStatJpaEntity> findByUserIdAndPlatformAndTimeClassAndYearAndMonth(
        Long userId, OAuthPlatForm platform, String timeClass, int year, int month);

    boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}