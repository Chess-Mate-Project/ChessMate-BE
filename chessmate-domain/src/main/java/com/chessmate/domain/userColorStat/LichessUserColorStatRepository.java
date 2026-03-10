package com.chessmate.domain.userColorStat;


import com.chessmate.common.type.GameType;
import java.util.List;

public interface LichessUserColorStatRepository {
  void saveAll(Iterable<LichessUserColorStat> entities);
  List<LichessUserColorStat> findByUserIdAndGameType(Long userId, GameType gameType);

  void deleteAllByUserId(Long userId);
}

