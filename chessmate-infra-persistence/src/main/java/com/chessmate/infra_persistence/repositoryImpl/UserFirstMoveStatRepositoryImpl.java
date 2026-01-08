package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStat;
import com.chessmate.domain.userFirstMoveStat.UserFirstMoveStatRepository;
import com.chessmate.infra_persistence.jpaRepository.UserFirstMoveStatJpaRepository;
import com.chessmate.infra_persistence.mapper.UserFirstMoveStatMapper;
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
}
