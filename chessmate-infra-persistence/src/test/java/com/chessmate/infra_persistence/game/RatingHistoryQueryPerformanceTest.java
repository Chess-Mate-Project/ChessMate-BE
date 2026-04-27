package com.chessmate.infra_persistence.game;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.infra_persistence.stat.entity.UserMonthlyRatingStatJpaEntity;
import com.chessmate.infra_persistence.stat.jpaRepository.UserMonthlyRatingStatJpaRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fix-134: user_monthly_rating_stat 테이블 기반 rating-history 조회 검증.
 *
 * 기존 GameRepository.findMonthlyLastRating()의 O(N²) 코릴레이티드 서브쿼리를
 * 사전 집계 테이블로 교체하면서 이 테스트도 새 구조에 맞게 재작성.
 */
@DataJpaTest
class RatingHistoryQueryPerformanceTest {

    @Autowired
    private UserMonthlyRatingStatJpaRepository monthlyRatingStatRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long USER_ID = 1L;
    private static final OAuthPlatForm PLATFORM = OAuthPlatForm.LICHESS;

    @Test
    @DisplayName("[정상] 12개월 × 3 타임클래스 데이터에서 월별 레이팅 전체 반환")
    void should_return_monthly_ratings_correctly() {
        // given: 12개월 × 3 타임클래스 = 36건
        String[] timeClasses = {"blitz", "rapid", "bullet"};
        for (int month = 1; month <= 12; month++) {
            for (String tc : timeClasses) {
                entityManager.persist(UserMonthlyRatingStatJpaEntity.builder()
                    .userId(USER_ID)
                    .platform(PLATFORM)
                    .timeClass(tc)
                    .year(2025)
                    .month(month)
                    .rating(1500 + month * 10)
                    .build());
            }
        }
        entityManager.flush();
        entityManager.clear();

        // when
        List<UserMonthlyRatingStatJpaEntity> result =
            monthlyRatingStatRepository.findByUserIdAndPlatform(USER_ID, PLATFORM);

        // then: 36행, 12월 레이팅 = 1500 + 12*10 = 1620
        assertThat(result).hasSize(36);
        assertThat(result)
            .filteredOn(r -> r.getMonth() == 12)
            .allMatch(r -> r.getRating() == 1620);
    }

    @Test
    @DisplayName("[정상] findByUserIdAndPlatformAndTimeClassAndYearAndMonth — 증분 업데이트 키 조회")
    void should_find_by_composite_key_for_incremental_update() {
        // given
        entityManager.persist(UserMonthlyRatingStatJpaEntity.builder()
            .userId(USER_ID).platform(PLATFORM)
            .timeClass("blitz").year(2025).month(4)
            .rating(1600)
            .build());
        entityManager.flush();
        entityManager.clear();

        // when: 존재하는 행
        Optional<UserMonthlyRatingStatJpaEntity> found =
            monthlyRatingStatRepository.findByUserIdAndPlatformAndTimeClassAndYearAndMonth(
                USER_ID, PLATFORM, "blitz", 2025, 4);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getRating()).isEqualTo(1600);

        // when: 존재하지 않는 행 (월이 다름)
        Optional<UserMonthlyRatingStatJpaEntity> notFound =
            monthlyRatingStatRepository.findByUserIdAndPlatformAndTimeClassAndYearAndMonth(
                USER_ID, PLATFORM, "blitz", 2025, 5);

        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("[정상] existsByUserIdAndPlatform — 집계 데이터 존재 여부 체크")
    void should_return_false_when_no_data_exists() {
        // given: 데이터 없음
        assertThat(monthlyRatingStatRepository.existsByUserIdAndPlatform(USER_ID, PLATFORM)).isFalse();

        // when: 데이터 추가
        entityManager.persist(UserMonthlyRatingStatJpaEntity.builder()
            .userId(USER_ID).platform(PLATFORM)
            .timeClass("blitz").year(2025).month(1)
            .rating(1500)
            .build());
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(monthlyRatingStatRepository.existsByUserIdAndPlatform(USER_ID, PLATFORM)).isTrue();
    }

    @Test
    @DisplayName("[정상] deleteByUserIdAndPlatform — 해당 유저 데이터만 삭제")
    void should_delete_only_target_user_data() {
        // given: 두 유저 데이터
        entityManager.persist(UserMonthlyRatingStatJpaEntity.builder()
            .userId(USER_ID).platform(PLATFORM)
            .timeClass("blitz").year(2025).month(1).rating(1500).build());
        entityManager.persist(UserMonthlyRatingStatJpaEntity.builder()
            .userId(2L).platform(PLATFORM)
            .timeClass("blitz").year(2025).month(1).rating(1600).build());
        entityManager.flush();
        entityManager.clear();

        // when
        monthlyRatingStatRepository.deleteByUserIdAndPlatform(USER_ID, PLATFORM);

        // then: USER_ID 데이터만 삭제되고 2L 데이터는 유지
        assertThat(monthlyRatingStatRepository.findByUserIdAndPlatform(USER_ID, PLATFORM)).isEmpty();
        assertThat(monthlyRatingStatRepository.findByUserIdAndPlatform(2L, PLATFORM)).hasSize(1);
    }

    /**
     * 다수 유저 데이터 중 단일 유저 조회 시 인덱스(user_id, platform)가 작동하는지 검증.
     * 구 방식: Game 30,000건 코릴레이티드 서브쿼리 O(N²)
     * 신 방식: 36,000건 집계 테이블에서 인덱스 조회 O(1)
     */
    @Test
    @DisplayName("[성능] 1,000 유저 × 36행 = 36,000건에서 단일 유저 조회 속도 검증")
    void index_lookup_should_be_fast_with_large_dataset() {
        // given: 1,000 유저 × 12개월 × 3 타임클래스 = 36,000행
        String[] timeClasses = {"blitz", "rapid", "bullet"};
        for (long uid = 1; uid <= 1000; uid++) {
            for (int month = 1; month <= 12; month++) {
                for (String tc : timeClasses) {
                    entityManager.persist(UserMonthlyRatingStatJpaEntity.builder()
                        .userId(uid).platform(PLATFORM)
                        .timeClass(tc).year(2025).month(month)
                        .rating(1500 + (int)(uid % 300))
                        .build());
                }
            }
            if (uid % 100 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        entityManager.flush();
        entityManager.clear();

        // when
        long startNs = System.nanoTime();
        List<UserMonthlyRatingStatJpaEntity> result =
            monthlyRatingStatRepository.findByUserIdAndPlatform(USER_ID, PLATFORM);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        System.out.printf(
            "%n[PERF] 36,000건 중 단일 유저 조회: %dms | 결과 rows: %d%n",
            elapsedMs, result.size()
        );

        // then
        assertThat(result).hasSize(36);
        assertThat(elapsedMs)
            .as("인덱스 조회 %dms — 100ms 이하여야 함", elapsedMs)
            .isLessThan(100L);
    }
}