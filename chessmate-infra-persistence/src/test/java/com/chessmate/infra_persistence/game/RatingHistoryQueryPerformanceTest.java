package com.chessmate.infra_persistence.game;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.GameResult;
import com.chessmate.infra_persistence.game.entity.GameJpaEntity;
import com.chessmate.infra_persistence.game.jpaRepository.GameJpaRepository;
import com.chessmate.infra_persistence.game.projection.MonthlyRatingProjection;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * rating-history 쿼리의 성능 문제를 재현하는 테스트.
 *
 * <p>문제: {@code findMonthlyLastRating}의 코릴레이티드 서브쿼리가
 * 게임 건수가 늘어날수록 O(N²)으로 느려짐.
 *
 * <p>원인:
 * <pre>
 *   WHERE g.playedAt = (
 *       SELECT MAX(g2.playedAt) FROM game g2
 *       WHERE g2.timeClass = g.timeClass
 *         AND YEAR(g2.playedAt) = YEAR(g.playedAt)
 *         AND MONTH(g2.playedAt) = MONTH(g.playedAt) ...
 *   )
 * </pre>
 * 외부 쿼리의 후보 행마다 서브쿼리가 한 번씩 실행되고,
 * YEAR()/MONTH() 함수 호출로 인해 인덱스도 활용되지 않음.
 *
 * <p>수정 방향: 코릴레이티드 서브쿼리 → GROUP BY + MAX() 또는 윈도우 함수(ROW_NUMBER)로 교체.
 */
@DataJpaTest
class RatingHistoryQueryPerformanceTest {

    @Autowired
    private GameJpaRepository gameJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long USER_ID = 1L;
    private static final OAuthPlatForm PLATFORM = OAuthPlatForm.LICHESS;
    private static final int GAME_COUNT = 30_000;

    /**
     * 소량 데이터(360건)에서 쿼리 정확도를 검증한다.
     * 이 테스트는 항상 통과해야 한다.
     */
    @Test
    @DisplayName("[정상] 소량 데이터에서 월별 마지막 레이팅 반환 검증")
    void should_return_monthly_last_rating_correctly() {
        // given: 12개월 × 3 타임클래스 × 10건 = 360건
        LocalDateTime base = LocalDateTime.now().minusMonths(11)
            .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        String[] timeClasses = {"blitz", "rapid", "bullet"};

        for (int month = 0; month < 12; month++) {
            for (String tc : timeClasses) {
                for (int day = 0; day < 10; day++) {
                    entityManager.persist(GameJpaEntity.builder()
                        .userId(USER_ID)
                        .platform(PLATFORM)
                        .platformGameId("small-" + month + "-" + tc + "-" + day)
                        .username("tester")
                        .opponentUsername("opp")
                        .playerColor("WHITE")
                        .result(GameResult.WIN)
                        .timeClass(tc)
                        .timeControl("300+0")
                        .rated(true)
                        .rating(1500 + day * 10)
                        .moves("e4 e5")
                        .variant("standard")
                        .playedAt(base.plusMonths(month).plusDays(day))
                        .createdAt(LocalDateTime.now())
                        .build());
                }
            }
        }
        entityManager.flush();
        entityManager.clear();

        LocalDateTime since = base;

        // when
        List<MonthlyRatingProjection> result =
            gameJpaRepository.findMonthlyLastRating(USER_ID, PLATFORM, null, since);

        // then: 12개월 × 3 타임클래스 = 36행
        assertThat(result).hasSize(36);
        // 각 월의 마지막 게임(day=9) 레이팅: 1500 + 9*10 = 1590
        assertThat(result).allMatch(r -> r.getRating() == 1590);
    }

    /**
     * 대용량 데이터(30,000건)에서 코릴레이티드 서브쿼리 성능 병목을 재현한다.
     *
     * <p>이 테스트는 현재 실패한다 — 코릴레이티드 서브쿼리로 인해 500ms를 초과하기 때문.
     * 쿼리를 GROUP BY + MAX() 또는 윈도우 함수로 교체한 뒤에는 통과해야 한다.
     */
    @Test
    @DisplayName("[재현] 대용량 Game 30,000건에서 rating-history 코릴레이티드 서브쿼리 지연")
    void reproduction_correlatedSubquery_slowness_with_large_dataset() {
        // given
        insertGames(GAME_COUNT);

        LocalDateTime since = LocalDateTime.now().minusMonths(11)
            .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // when
        long startNs = System.nanoTime();
        List<MonthlyRatingProjection> result =
            gameJpaRepository.findMonthlyLastRating(USER_ID, PLATFORM, null, since);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        // then
        System.out.printf(
            "%n[PERF] game %,d건 → findMonthlyLastRating: %dms | 결과 rows: %d%n",
            GAME_COUNT, elapsedMs, result.size()
        );

        assertThat(result).isNotEmpty();

        // 수정 후 목표 임계치: 500ms 이하.
        // 코릴레이티드 서브쿼리 상태에서는 이 assertion이 실패함 → 버그 재현됨.
        assertThat(elapsedMs)
            .as("쿼리 응답시간 %dms — 500ms 이하여야 함 (현재: 코릴레이티드 서브쿼리 병목으로 초과)", elapsedMs)
            .isLessThan(500L);
    }

    private void insertGames(int count) {
        String[] timeClasses = {"blitz", "rapid", "bullet"};
        LocalDateTime base = LocalDateTime.now().minusMonths(11)
            .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        for (int i = 0; i < count; i++) {
            entityManager.persist(GameJpaEntity.builder()
                .userId(USER_ID)
                .platform(PLATFORM)
                .platformGameId("perf-game-" + i)
                .username("tester")
                .opponentUsername("opp-" + i)
                .playerColor(i % 2 == 0 ? "WHITE" : "BLACK")
                .result(GameResult.WIN)
                .timeClass(timeClasses[i % timeClasses.length])
                .timeControl("300+0")
                .rated(true)
                .rating(1500 + (i % 300))
                .moves("e4 e5")
                .variant("standard")
                .playedAt(base.plusHours(i))
                .createdAt(LocalDateTime.now())
                .build());

            if ((i + 1) % 500 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();
    }
}