package com.chessmate.domain.userFirstMoveStat;

import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameType;
import java.util.List;

public interface UserFirstMoveStatRepository {
  void saveAll(Iterable<UserFirstMoveStat> entities);
  List<UserFirstMoveStat> findByUserIdAndGameType(Long userId, GameType gameType);
  List<UserFirstMoveStat> findByUserIdAndGameTypeAndColor(Long userId, GameType gameType, ChessColor color);
}