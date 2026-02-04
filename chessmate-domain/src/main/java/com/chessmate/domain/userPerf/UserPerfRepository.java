package com.chessmate.domain.userPerf;

import com.chessmate.common.type.GameType;
import java.util.Optional;

public interface UserPerfRepository {
  UserPerf save(UserPerf userPerf);

  Optional<UserPerf> findByUserIdAndGameType(Long userId, GameType gameType);
}
