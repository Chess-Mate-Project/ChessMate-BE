package com.chessmate.infra_persistence.chesscom.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chesscom_users")
public class ChesscomUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "chesscom_id", nullable = false, unique = true)
  private Long chesscomId;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "banner", columnDefinition = "TEXT")
  private String banner;

  @Column(name = "profile", columnDefinition = "TEXT")
  private String profile;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "platform_joined_at")
  private LocalDateTime platformJoinedAt;

  // Constructors
  public ChesscomUserEntity() {}

  // Getters
  public Long getId() {
    return id;
  }

  public Long getChesscomId() {
    return chesscomId;
  }

  public String getUsername() {
    return username;
  }

  public String getDescription() {
    return description;
  }

  public String getBanner() {
    return banner;
  }

  public String getProfile() {
    return profile;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public LocalDateTime getPlatformJoinedAt() {
    return platformJoinedAt;
  }

  // Setters
  public void setId(Long id) {
    this.id = id;
  }

  public void setChesscomId(Long chesscomId) {
    this.chesscomId = chesscomId;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setBanner(String banner) {
    this.banner = banner;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setPlatformJoinedAt(LocalDateTime platformJoinedAt) {
    this.platformJoinedAt = platformJoinedAt;
  }

  // Builder
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private Long chesscomId;
    private String username;
    private String description;
    private String banner;
    private String profile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime platformJoinedAt;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder chesscomId(Long chesscomId) {
      this.chesscomId = chesscomId;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder banner(String banner) {
      this.banner = banner;
      return this;
    }

    public Builder profile(String profile) {
      this.profile = profile;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder platformJoinedAt(LocalDateTime platformJoinedAt) {
      this.platformJoinedAt = platformJoinedAt;
      return this;
    }

    public ChesscomUserEntity build() {
      ChesscomUserEntity entity = new ChesscomUserEntity();
      entity.id = this.id;
      entity.chesscomId = this.chesscomId;
      entity.username = this.username;
      entity.description = this.description;
      entity.banner = this.banner;
      entity.profile = this.profile;
      entity.createdAt = this.createdAt;
      entity.updatedAt = this.updatedAt;
      entity.platformJoinedAt = this.platformJoinedAt;
      return entity;
    }
  }
}
