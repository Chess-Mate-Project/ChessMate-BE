package com.chessmate.domain.user;

import java.time.LocalDateTime;

public class User {
  private Long id;
  private String lichessId;
  private String username;
  private String description;
  private String BannerImage;
  private String profileImage;
  private String title;
  private LocalDateTime createdAt;

  // 무인자 생성자(프레임워크/직렬화용)
  public User() {}

  private User(Builder builder) {
    this.id = builder.id;
    this.lichessId = builder.lichessId;
    this.username = builder.username;
    this.description = builder.description;
    this.title = builder.title;
    this.createdAt = builder.createdAt;
  }

  public static Builder builder() {
    return new Builder();
  }

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

  public String getTitle() {
    return title;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  // Setter들
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

  public void setTitle(String title) {
    this.title = title;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getBannerImage() {
    return BannerImage;
  }

  public void setBannerImage(String bannerImage) {
    BannerImage = bannerImage;
  }

  public String getProfileImage() {
    return profileImage;
  }

  public void setProfileImage(String profileImage) {
    this.profileImage = profileImage;
  }

  public static class Builder {
    private Long id;
    private String lichessId;
    private String username;
    private String description;
    private String title;
    private LocalDateTime createdAt;

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

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public User build() {
      return new User(this);
    }
  }
}
