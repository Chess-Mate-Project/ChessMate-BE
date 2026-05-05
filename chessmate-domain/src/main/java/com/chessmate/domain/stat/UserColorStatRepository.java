package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.List;
import java.util.Optional;

public interface UserColorStatRepository {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserColorStat> saveAll(List<UserColorStat> stats);
    List<UserColorStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    Optional<UserColorStat> findByUserIdAndPlatformAndTimeClassAndColor(Long userId, OAuthPlatForm platform, String timeClass, String color);

    List<UserColorStat> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);

    boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}