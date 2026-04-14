package com.chessmate.infra_persistence.lichess.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lichess_users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class LichessUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lichess_id", nullable = false, unique = true)
  private String lichessId;

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "description")
  private String description;

  @Column(name = "banner_image")
  private String bannerImage;

  @Column(name = "profile_image")
  private String profileImage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "platform_joined_at")
  private LocalDateTime platformJoinedAt;
}
