package com.chessmate.domain.userPerf;

import com.chessmate.common.type.GameType;
import java.util.List;
import java.util.Optional;

public interface UserPerfRepository {
  UserPerf save(UserPerf userPerf);

  Optional<UserPerf> findByUserIdAndGameType(Long userId, GameType gameType);

  List<UserPerf> findRankingByGameType(GameType gameType);

  int countUsersBetterRating(GameType gameType, int rating);
}
