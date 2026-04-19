package com.chessmate.infra_persistence.lichess.user.mapper;

import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.infra_persistence.lichess.user.entity.LichessUserEntity;
import org.springframework.stereotype.Component;

@Component
public class LichessUserMapper {

  public LichessUserEntity toEntity(LichessUser user) {
    if (user == null) return null;

    LichessUserEntity entity = LichessUserEntity.builder()
        .lichessId(user.getLichessId())
        .username(user.getUsername())
        .description(user.getDescription())
        .bannerImage(user.getBanner())
        .profileImage(user.getProfile())
        .createdAt(user.getCreatedAt())
        .platformJoinedAt(user.getPlatformJoinedAt())
        .deletedAt(user.getDeletedAt())
        .build();

    if (user.getId() != null) {
      entity.setId(user.getId());
    }

    return entity;
  }

  public LichessUser toDomain(LichessUserEntity entity) {
    if (entity == null) return null;

    return LichessUser.builder()
        .id(entity.getId())
        .lichessId(entity.getLichessId())
        .username(entity.getUsername())
        .description(entity.getDescription())
        .banner(entity.getBannerImage())
        .profile(entity.getProfileImage())
        .createdAt(entity.getCreatedAt())
        .platformJoinedAt(entity.getPlatformJoinedAt())
        .deletedAt(entity.getDeletedAt())
        .build();
  }
}
