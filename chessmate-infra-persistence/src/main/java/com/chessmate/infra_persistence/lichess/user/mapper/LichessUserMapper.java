package com.chessmate.infra_persistence.lichess.user.mapper;

import com.chessmate.domain.lichess.user.LichessUser;
import com.chessmate.infra_persistence.lichess.user.entity.LichessUserEntity;
import org.springframework.stereotype.Component;

/**
 * Lichess 사용자 도메인 <-> Entity 매퍼
 * - 도메인 모델과 JPA 엔티티 간의 상호 변환을 담당
 */
@Component
public class LichessUserMapper {

  /**
   * LichessUser 도메인 모델을 LichessUserEntity로 변환
   *
   * @param user Lichess 사용자 도메인 모델
   * @return 변환된 LichessUserEntity
   */
  public LichessUserEntity toEntity(LichessUser user) {
    if (user == null) {
      return null;
    }

    LichessUserEntity entity = LichessUserEntity.builder()
        .lichessId(user.getLichessId())
        .username(user.getUsername())
        .description(user.getDescription())
        .bannerImage(user.getBanner())
        .profileImage(user.getProfile())
        .createdAt(user.getCreatedAt())
        .build();

    if (user.getId() != null) {
      entity.setId(user.getId());
    }

    return entity;
  }

  /**
   * LichessUserEntity를 LichessUser 도메인 모델로 변환
   *
   * @param entity JPA 엔티티
   * @return 변환된 Lichess 사용자 도메인 모델
   */
  public LichessUser toDomain(LichessUserEntity entity) {
    if (entity == null) {
      return null;
    }

    return LichessUser.builder()
        .id(entity.getId())
        .lichessId(entity.getLichessId())
        .username(entity.getUsername())
        .description(entity.getDescription())
        .banner(entity.getBannerImage())
        .profile(entity.getProfileImage())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}

