package com.chessmate.infra_core.entity;

import jakarta.persistence.Access;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "profiles")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Profile {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user; // 어떤 유저의 프로필인가?

  @Column(name = "description")
  private String description;

  @Column(name = "profile_image")
  private String profileImage;

  @Column(name = "banner_image")
  private String bannerImage;
  @Enumerated(EnumType.STRING)
  private OAuthPlatForm platform; // LICHESS, CHESSCOM 등

  private String username; // 플랫폼별 닉네임
  private String platformId; // 플랫폼별 고유 ID
  private String profileUrl;
  private String bannerUrl;
  private LocalDateTime platformCreatedAt;
}