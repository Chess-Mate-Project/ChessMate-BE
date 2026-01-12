package com.chessmate.infra_persistence.mapper;

import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.entity.UserEntity;

public class UserMapper {

  public static User toDomain(UserEntity entity) {
    if(entity == null) return null;
    return User.builder()
        .id(entity.getId())
        .username(entity.getUsername())
        .lichessId(entity.getLichessId())
        .bannerImage(entity.getBannerImage())
        .profileImage(entity.getProfileImage())
        .lichessCreatedAt(entity.getLichessCreatedAt())
        .build();
  }

  public static UserEntity toEntity(User domain) {
    if(domain == null) return null;
    return UserEntity.builder()
        .id(domain.getId())
        .username(domain.getUsername())
        .lichessId(domain.getLichessId())
        .bannerImage(domain.getBannerImage())
        .profileImage(domain.getProfileImage())
        .lichessCreatedAt(domain.getLichessCreatedAt())
        .build();
  }
}
