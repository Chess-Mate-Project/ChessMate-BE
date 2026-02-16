package com.chessmate.infra_persistence.jpaRepository;

import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.infra_persistence.entity.UserFirstMoveStatEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFirstMoveStatJpaRepository extends
    JpaRepository<UserFirstMoveStatEntity, Long> {
  List<UserFirstMoveStatEntity> findByUserIdAndGameType(Long userId, GameType gameType);
  List<UserFirstMoveStatEntity> findByUserIdAndGameTypeAndColor(Long userId, GameType gameType, ChessColor color);

  void deleteByUserId(Long userId);
}
