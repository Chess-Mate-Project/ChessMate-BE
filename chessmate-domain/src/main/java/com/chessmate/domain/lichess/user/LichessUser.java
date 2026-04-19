package com.chessmate.domain.lichess.user;

import java.time.LocalDateTime;

public class LichessUser {
  private Long id;
  private String lichessId;
  private String username;
  private String description;
  private String banner;
  private String profile;
  private LocalDateTime createdAt;
  private LocalDateTime platformJoinedAt;
  private LocalDateTime deletedAt;

  public boolean isDeleted() {
    return this.deletedAt != null;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.username = "deleted_" + this.id;
    this.description = null;
    this.banner = null;
    this.profile = null;
  }

  public void restore(String freshUsername) {
    this.deletedAt = null;
    this.username = freshUsername;
  }

  public static LichessUser newUser(String lichessId, String username) {

    return LichessUser.builder()
        .id(null) // domain에서 id를 정의해야하는가?
        .lichessId(lichessId)
        .username(username)
        .createdAt(LocalDateTime.now())
        .profile(null)
        .banner(null)
        .description(null)
        .build();
  }

  // Getters
  public Long getId() {
    return id;
  }

  public String getLichessId() {
    return lichessId;
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

  public LocalDateTime getPlatformJoinedAt() {
    return platformJoinedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  // Setters
  public void setId(Long id) {
    this.id = id;
  }

  public void setLichessId(String lichessId) {
    this.lichessId = lichessId;
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

  public void setPlatformJoinedAt(LocalDateTime platformJoinedAt) {
    this.platformJoinedAt = platformJoinedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  // Builder
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private String lichessId;
    private String username;
    private String description;
    private String banner;
    private String profile;
    private LocalDateTime createdAt;
    private LocalDateTime platformJoinedAt;
    private LocalDateTime deletedAt;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder lichessId(String lichessId) {
      this.lichessId = lichessId;
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

    public Builder platformJoinedAt(LocalDateTime platformJoinedAt) {
      this.platformJoinedAt = platformJoinedAt;
      return this;
    }

    public Builder deletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public LichessUser build() {
      LichessUser user = new LichessUser();
      user.id = this.id;
      user.lichessId = this.lichessId;
      user.username = this.username;
      user.description = this.description;
      user.banner = this.banner;
      user.profile = this.profile;
      user.createdAt = this.createdAt;
      user.platformJoinedAt = this.platformJoinedAt;
      user.deletedAt = this.deletedAt;
      return user;
    }
  }
}

