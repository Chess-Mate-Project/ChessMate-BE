package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.List;

public interface UserFirstMoveStatRepository {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserFirstMoveStat> saveAll(List<UserFirstMoveStat> stats);
    List<UserFirstMoveStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    List<UserFirstMoveStat> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);
}