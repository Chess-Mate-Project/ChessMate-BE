package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserDailyGameStatRepository {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserDailyGameStat> saveAll(List<UserDailyGameStat> stats);
    List<UserDailyGameStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    Optional<UserDailyGameStat> findByUserIdAndPlatformAndDate(Long userId, OAuthPlatForm platform, LocalDate date);

    List<UserDailyGameStat> findByUserIdAndPlatformAndYear(Long userId, OAuthPlatForm platform, int year);

    boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}