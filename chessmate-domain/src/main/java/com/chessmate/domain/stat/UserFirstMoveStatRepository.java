package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.List;
import java.util.Optional;

public interface UserFirstMoveStatRepository {
    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    List<UserFirstMoveStat> saveAll(List<UserFirstMoveStat> stats);
    List<UserFirstMoveStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
    Optional<UserFirstMoveStat> findByUserIdAndPlatformAndTimeClassAndColorAndMove(Long userId, OAuthPlatForm platform, String timeClass, String color, String move);

    List<UserFirstMoveStat> findByUserIdAndPlatformAndTimeClass(Long userId, OAuthPlatForm platform, String timeClass);

    boolean existsByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}