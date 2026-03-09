package com.chessmate.infra_core.entity;

import jakarta.persistence.Access;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "profile") // 모든 데이터가 이 테이블 하나에 담깁니다.
@Inheritance(strategy = InheritanceType.SINGLE_TABLE) // 전략 명시
@DiscriminatorColumn(name = "platform_type") // 자식을 구분할 컬럼 이름
@Getter @Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Profile {

  private final AccessLevel accessLevel = AccessLevel.PROTECTED;
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