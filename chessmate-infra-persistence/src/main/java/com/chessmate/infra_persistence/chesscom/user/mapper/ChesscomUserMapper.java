package com.chessmate.infra_persistence.chesscom.user.mapper;

import com.chessmate.domain.chesscom.user.ChesscomUser;
import com.chessmate.infra_persistence.chesscom.user.entity.ChesscomUserEntity;
import org.springframework.stereotype.Component;

/**
 * Chess.com 사용자 도메인 <-> Entity 매퍼
 * - 도메인 모델과 JPA 엔티티 간의 상호 변환을 담당
 */
@Component
public class ChesscomUserMapper {

  /**
   * ChesscomUser 도메인 모델을 ChesscomUserEntity로 변환
   *
   * @param user Chess.com 사용자 도메인 모델
   * @return 변환된 ChesscomUserEntity
   */
  public ChesscomUserEntity toEntity(ChesscomUser user) {
    if (user == null) {
      return null;
    }

    ChesscomUserEntity entity = ChesscomUserEntity.builder()
        .chesscomId(user.getChesscomId())
        .username(user.getUsername())
        .description(user.getDescription())
        .banner(user.getBanner())
        .profile(user.getProfile())
        .createdAt(user.getCreatedAt())
        .build();

    if (user.getId() != null) {
      entity.setId(user.getId());
    }

    return entity;
  }

  /**
   * ChesscomUserEntity를 ChesscomUser 도메인 모델로 변환
   *
   * @param entity JPA 엔티티
   * @return 변환된 Chess.com 사용자 도메인 모델
   */
  public ChesscomUser toDomain(ChesscomUserEntity entity) {
    if (entity == null) {
      return null;
    }

    return ChesscomUser.builder()
        .id(entity.getId())
        .chesscomId(entity.getChesscomId())
        .username(entity.getUsername())
        .description(entity.getDescription())
        .banner(entity.getBanner())
        .profile(entity.getProfile())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}

