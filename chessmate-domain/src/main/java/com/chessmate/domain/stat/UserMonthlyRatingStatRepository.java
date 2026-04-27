package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.List;
import java.util.Optional;

public interface UserMonthlyRatingStatRepository {
    List<UserMonthlyRatingStat> saveAll(List<UserMonthlyRatingStat> stats);
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserMonthlyRatingStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    Optional<UserMonthlyRatingStat> findByUserIdAndPlatformAndTimeClassAndYearAndMonth(
        Long userId, OAuthPlatForm platform, String timeClass, int year, int month);
    boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}