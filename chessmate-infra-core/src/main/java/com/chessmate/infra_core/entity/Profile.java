package com.chessmate.infra_core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@MappedSuperclass // JPA 상속 매핑의 핵심
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Profile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "profile")
  private String profile;

  @Column(name = "banner")
  private String banner;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // 생성 시 자동으로 날짜 주입
  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  //업데이트시에 자동으로 날짜를 주입
  @PreUpdate
  public void preUpdate() {
    this.updatedAt = LocalDateTime.now();
  }


//  public abstract Long getId();
  public abstract OauthPlatForm getProvider();
}