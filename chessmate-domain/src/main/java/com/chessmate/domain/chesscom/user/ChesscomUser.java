package com.chessmate.domain.chesscom.user;

import java.time.LocalDateTime;

public class ChesscomUser {
  private Long id;
  private Long chesscomId;
  private String username;
  private String description;
  private String banner;
  private String profile;
  private LocalDateTime createdAt;
  private LocalDateTime platformJoinedAt;

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

    public Builder platformJoinedAt(LocalDateTime platformJoinedAt) {
      this.platformJoinedAt = platformJoinedAt;
      return this;
    }

    public ChesscomUser build() {
      ChesscomUser user = new ChesscomUser();
      user.id = this.id;
      user.chesscomId = this.chesscomId;
      user.username = this.username;
      user.description = this.description;
      user.banner = this.banner;
      user.profile = this.profile;
      user.createdAt = this.createdAt;
      user.platformJoinedAt = this.platformJoinedAt;
      return user;
    }
  }

}
