package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserPerfStatRepository {

    List<UserPerfStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    List<UserPerfStat> saveAll(List<UserPerfStat> stats);

    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    Optional<UserPerfStat> findTopRatingByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    /**
     * 여러 userId에 대해 플랫폼별 최고 레이팅 스탯을 한 번에 조회 (N+1 방지용)
     * key: userId, value: 해당 유저의 최고 레이팅 stat
     */
    Map<Long, UserPerfStat> findTopRatingByUserIdsAndPlatform(List<Long> userIds, OAuthPlatForm platform);

    /**
     * 플랫폼 + 게임 타입별 전체 랭킹 조회
     * ORDER BY rating DESC, userId ASC (동점자는 userId 오름차순)
     */
    List<UserPerfStat> findRankingByPlatformAndTimeClass(OAuthPlatForm platform, String timeClass);

    /**
     * 특정 유저의 플랫폼 + 게임 타입 성능 조회
     */
    Optional<UserPerfStat> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);
}
