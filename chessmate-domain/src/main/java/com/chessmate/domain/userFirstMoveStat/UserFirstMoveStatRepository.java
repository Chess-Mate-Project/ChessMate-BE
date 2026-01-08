package com.chessmate.domain.userFirstMoveStat;

public interface UserFirstMoveStatRepository {
  void saveAll(Iterable<UserFirstMoveStat> entities);
}