package com.chessmate.infra_persistence.repositoryImpl;

import com.chessmate.domain.userPerf.UserPerf;
import com.chessmate.domain.userPerf.UserPerfRepository;
import com.chessmate.common.type.GameType;
import com.chessmate.infra_persistence.entity.UserPerfEntity;
import com.chessmate.infra_persistence.jpaRepository.UserPerfJpaRepository;
import com.chessmate.infra_persistence.mapper.UserPerfMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPerfRepositoryImpl implements UserPerfRepository {

  private final UserPerfJpaRepository jpaRepository;

  @Override
  public UserPerf save(UserPerf userPerf) {
    UserPerfEntity entity = UserPerfMapper.toEntity(userPerf);
    UserPerfEntity saved = jpaRepository.save(entity);
    return UserPerfMapper.toDomain(saved);
  }

  @Override
  public Optional<UserPerf> findByUserIdAndGameType(Long userId, GameType gameType) {
    return jpaRepository.findByUserIdAndGameType(userId, gameType)
        .map(UserPerfMapper::toDomain);
  }
}

