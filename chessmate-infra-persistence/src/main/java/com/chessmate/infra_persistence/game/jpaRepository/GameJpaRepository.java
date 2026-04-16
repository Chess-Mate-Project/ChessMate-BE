package com.chessmate.infra_persistence.game.jpaRepository;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.game.entity.GameJpaEntity;
import com.chessmate.infra_persistence.game.projection.MonthlyRatingProjection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameJpaRepository extends JpaRepository<GameJpaEntity, Long> {

    @Query("SELECT g.platformGameId FROM GameJpaEntity g WHERE g.platform = :platform AND g.platformGameId IN :ids")
    List<String> findExistingPlatformGameIds(
        @Param("platform") OAuthPlatForm platform,
        @Param("ids") List<String> ids
    );

    List<GameJpaEntity> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    /** 증분 수집용: 해당 유저의 플랫폼 내 가장 최근 played_at 조회 */
    @Query("SELECT MAX(g.playedAt) FROM GameJpaEntity g WHERE g.userId = :userId AND g.platform = :platform")
    Optional<LocalDateTime> findLatestPlayedAtByUserIdAndPlatform(
        @Param("userId") Long userId,
        @Param("platform") OAuthPlatForm platform
    );

    /**
     * 최근 1년간 월별 마지막 레이팅 조회.
     * 각 (year, month) 그룹에서 played_at이 가장 늦은 게임의 rating을 반환.
     */
    @Query("""
        SELECT g.timeClass AS timeClass,
               YEAR(g.playedAt) AS year,
               MONTH(g.playedAt) AS month,
               g.rating AS rating
        FROM GameJpaEntity g
        WHERE g.userId = :userId
          AND g.platform = :platform
          AND (:timeClass IS NULL OR g.timeClass = :timeClass)
          AND g.rated = true
          AND g.rating IS NOT NULL
          AND g.playedAt >= :since
          AND g.playedAt = (
              SELECT MAX(g2.playedAt)
              FROM GameJpaEntity g2
              WHERE g2.userId = g.userId
                AND g2.platform = g.platform
                AND g2.timeClass = g.timeClass
                AND YEAR(g2.playedAt) = YEAR(g.playedAt)
                AND MONTH(g2.playedAt) = MONTH(g.playedAt)
                AND g2.rating IS NOT NULL
          )
        ORDER BY YEAR(g.playedAt), MONTH(g.playedAt)
        """)
    List<MonthlyRatingProjection> findMonthlyLastRating(
        @Param("userId") Long userId,
        @Param("platform") OAuthPlatForm platform,
        @Param("timeClass") String timeClass,
        @Param("since") LocalDateTime since
    );
}