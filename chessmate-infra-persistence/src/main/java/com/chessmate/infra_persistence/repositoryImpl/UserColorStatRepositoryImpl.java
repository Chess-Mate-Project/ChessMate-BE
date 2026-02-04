package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.common.type.GameType;
import com.chessmate.domain.userColorStat.UserColorStat;
import com.chessmate.domain.userColorStat.UserColorStatRepository;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.infra_persistence.jpaRepository.UserColorStatJpaRepository;
import com.chessmate.infra_persistence.jpaRepository.UserFirstMoveStatJpaRepository;
import com.chessmate.infra_persistence.mapper.UserColorStatMapper;
import com.chessmate.infra_persistence.mapper.UserFirstMoveStatMapper;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserColorStatRepositoryImpl implements UserColorStatRepository
{
  private final UserColorStatJpaRepository jpaRepository;

  @Override
  public void saveAll(Iterable<UserColorStat> entities) {
    jpaRepository.saveAll(
        StreamSupport.stream(entities.spliterator(), false)
            .map(UserColorStatMapper::toEntity)
            .toList()
    );
  }

  @Override
  public List<UserColorStat> findByUserIdAndGameType(Long userId, GameType gameType) {
    return jpaRepository.findByUserIdAndGameType(userId, gameType).stream()
        .map(UserColorStatMapper::toDomain)
        .toList();
  }
}
