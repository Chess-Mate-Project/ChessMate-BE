package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.util.List;

public interface UserPerfStatRepository {

    List<UserPerfStat> findByUserIdAndPlatform(Long userId, OAuthPlatForm platform);

    List<UserPerfStat> saveAll(List<UserPerfStat> stats);

    void deleteByUserIdAndPlatform(Long userId, OAuthPlatForm platform);
}
