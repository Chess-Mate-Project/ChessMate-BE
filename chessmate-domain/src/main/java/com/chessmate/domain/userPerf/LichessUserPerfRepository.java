package com.chessmate.domain.userPerf;
}
  void deleteAllByUserId(Long userId);

  int countUsersBetterRating(GameType gameType, int rating);

  List<LichessUserPerf> findRankingByGameType(GameType gameType);

  Optional<LichessUserPerf> findByUserIdAndGameType(Long userId, GameType gameType);

  LichessUserPerf save(LichessUserPerf lichessUserPerf);
public interface LichessUserPerfRepository {

import java.util.Optional;
import java.util.List;
import com.chessmate.common.type.GameType;


