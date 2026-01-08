package com.chessmate.domain.userColorStat;


public interface UserColorStatRepository {
  void saveAll(Iterable<UserColorStat> entities);
}
