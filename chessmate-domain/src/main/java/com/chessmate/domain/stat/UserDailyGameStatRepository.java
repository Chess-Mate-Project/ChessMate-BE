package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.List;

public interface UserDailyGameStatRepository {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserDailyGameStat> saveAll(List<UserDailyGameStat> stats);
    List<UserDailyGameStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    List<UserDailyGameStat> findByUserIdAndPlatformAndYear(Long userId, OAuthPlatForm platform, int year);
}