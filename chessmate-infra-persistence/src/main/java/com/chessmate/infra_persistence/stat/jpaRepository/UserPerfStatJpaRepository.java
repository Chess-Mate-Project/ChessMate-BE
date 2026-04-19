package com.chessmate.infra_persistence.stat.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserPerfStatJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPerfStatJpaRepository extends JpaRepository<UserPerfStatJpaEntity, Long> {

    List<UserPerfStatJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserPerfStatJpaEntity s WHERE s.userId = :userId AND s.platform = :platform")
    void deleteByUserIdAndPlatform(@Param("userId") Long userId, @Param("platform") OAuthPlatForm platform);

    /**
     * 해당 유저/플랫폼에서 레이팅이 가장 높은 타임클래스 1건 조회.
     * timeClass와 rating을 함께 반환해야 하므로 엔티티 전체를 가져온 뒤 rating 기준 내림차순 첫 번째 행을 선택.
     */
    Optional<UserPerfStatJpaEntity> findFirstByUserIdAndPlatformOrderByRatingDesc(
        Long userId,
        OAuthPlatForm platform
    );

    @Query("SELECT s FROM UserPerfStatJpaEntity s WHERE s.userId IN :userIds AND s.platform = :platform")
    List<UserPerfStatJpaEntity> findByUserIdInAndPlatform(
        @Param("userIds") List<Long> userIds,
        @Param("platform") OAuthPlatForm platform
    );

    @Query("SELECT s FROM UserPerfStatJpaEntity s " +
           "WHERE s.platform = :platform AND s.timeClass = :timeClass " +
           "ORDER BY s.rating DESC, s.userId ASC")
    List<UserPerfStatJpaEntity> findRankingByPlatformAndTimeClass(
        @Param("platform") OAuthPlatForm platform,
        @Param("timeClass") String timeClass
    );

    Optional<UserPerfStatJpaEntity> findByUserIdAndPlatformAndTimeClass(
        Long userId, OAuthPlatForm platform, String timeClass
    );

    @Query("SELECT s FROM UserPerfStatJpaEntity s " +
           "WHERE s.platform = :platform AND s.timeClass = :timeClass " +
           "ORDER BY s.rating DESC, s.userId ASC")
    List<UserPerfStatJpaEntity> findRankingPageByPlatformAndTimeClass(
        @Param("platform") OAuthPlatForm platform,
        @Param("timeClass") String timeClass,
        Pageable pageable
    );

    long countByPlatformAndTimeClass(OAuthPlatForm platform, String timeClass);

    @Query("SELECT COUNT(s) FROM UserPerfStatJpaEntity s " +
           "WHERE s.platform = :platform AND s.timeClass = :timeClass " +
           "AND (s.rating > :rating OR (s.rating = :rating AND s.userId < :userId))")
    long countRankAbove(
        @Param("platform") OAuthPlatForm platform,
        @Param("timeClass") String timeClass,
        @Param("rating") int rating,
        @Param("userId") Long userId
    );
}
