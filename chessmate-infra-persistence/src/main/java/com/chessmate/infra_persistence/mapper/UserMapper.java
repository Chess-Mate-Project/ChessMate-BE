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
        .lastLoginAt(entity.getLastLoginAt())
        .createdAt(entity.getCreatedAt())
        .allGames(entity.getAllGames())
        .ratedGames(entity.getRatedGames())
        .wins(entity.getWins())
        .losses(entity.getLosses())
        .draws(entity.getDraws())
        .totalSeconds(entity.getTotalSeconds())
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
        .lastLoginAt(domain.getLastLoginAt())
        .createdAt(domain.getCreatedAt())
        .allGames(domain.getAllGames())
        .ratedGames(domain.getRatedGames())
        .wins(domain.getWins())
        .losses(domain.getLosses())
        .draws(domain.getDraws())
        .totalSeconds(domain.getTotalSeconds())
        .build();
  }
}
