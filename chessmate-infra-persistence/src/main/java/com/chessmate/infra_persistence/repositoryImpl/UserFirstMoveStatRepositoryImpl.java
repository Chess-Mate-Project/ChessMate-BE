package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.common.type.ChessColor;
import com.chessmate.common.type.GameType;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStatRepository;
import com.chessmate.infra_persistence.jpaRepository.UserFirstMoveStatJpaRepository;
import com.chessmate.infra_persistence.mapper.UserFirstMoveStatMapper;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserFirstMoveStatRepositoryImpl implements UserFirstMoveStatRepository {

  private final UserFirstMoveStatJpaRepository jpaRepository;

  @Override
  public void saveAll(Iterable<UserFirstMoveStat> entities) {
    jpaRepository.saveAll(
        StreamSupport.stream(entities.spliterator(), false)
            .map(UserFirstMoveStatMapper::toEntity)
            .toList()
    );
  }

  @Override
  public List<UserFirstMoveStat> findByUserIdAndGameType(Long userId, GameType gameType) {
    return jpaRepository.findByUserIdAndGameType(userId, gameType).stream()
        .map(UserFirstMoveStatMapper::toDomain)
        .toList();
  }

  @Override
  public List<UserFirstMoveStat> findByUserIdAndGameTypeAndColor(Long userId, GameType gameType, ChessColor color) {
    return jpaRepository.findByUserIdAndGameTypeAndColor(userId, gameType, color).stream()
        .map(UserFirstMoveStatMapper::toDomain)
        .toList();
  }
}
