package com.chessmate.domain.userColorStat;


import com.chessmate.common.type.GameType;
import java.util.List;

public interface UserColorStatRepository {
  void saveAll(Iterable<UserColorStat> entities);
  List<UserColorStat> findByUserIdAndGameType(Long userId, GameType gameType);

  void deleteAllByUserId(Long userId);
}
