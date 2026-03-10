package com.chessmate.domain.user;

import java.time.LocalDateTime;

public class LichessUser {
  private Long id;
  private String lichessId;
  private String username;
  private String description;
  private String bannerImage;
  private String profileImage;
  private String title;
  private LocalDateTime createdAt;
  private LocalDateTime lichessCreatedAt;
  private LocalDateTime lastLoginAt;

  // 게임 통계
  private int allGames;
  private int ratedGames;
  private int wins;
  private int losses;
  private int draws;
  private int totalSeconds;

  // 무인자 생성자
  public LichessUser() {}

  private LichessUser(Builder builder) {
    this.id = builder.id;
    this.lichessId = builder.lichessId;
    this.username = builder.username;
    this.description = builder.description;
    this.title = builder.title;
    this.createdAt = builder.createdAt;
    this.bannerImage = builder.bannerImage;
    this.profileImage = builder.profileImage;
    this.lichessCreatedAt = builder.lichessCreatedAt;
    this.lastLoginAt = builder.lastLoginAt;
    this.allGames = builder.allGames;
    this.ratedGames = builder.ratedGames;
    this.wins = builder.wins;
    this.losses = builder.losses;
    this.draws = builder.draws;
    this.totalSeconds = builder.totalSeconds;
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

  public String getBannerImage() {
    return bannerImage;
  }

  public String getProfileImage() {
    return profileImage;
  }

  public LocalDateTime getLichessCreatedAt() {
    return lichessCreatedAt;
  }

  public LocalDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  public int getAllGames() {
    return allGames;
  }

  public int getRatedGames() {
    return ratedGames;
  }

  public int getWins() {
    return wins;
  }

  public int getLosses() {
    return losses;
  }

  public int getDraws() {
    return draws;
  }

  public int getTotalSeconds() {
    return totalSeconds;
  }

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

  public void setBannerImage(String bannerImage) {
    this.bannerImage = bannerImage;
  }

  public void setProfileImage(String profileImage) {
    this.profileImage = profileImage;
  }

  public void setLichessCreatedAt(LocalDateTime lichessCreatedAt) {
    this.lichessCreatedAt = lichessCreatedAt;
  }

  public void setLastLoginAt(LocalDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public void setAllGames(int allGames) {
    this.allGames = allGames;
  }

  public void setRatedGames(int ratedGames) {
    this.ratedGames = ratedGames;
  }

  public void setWins(int wins) {
    this.wins = wins;
  }

  public void setLosses(int losses) {
    this.losses = losses;
  }

  public void setDraws(int draws) {
    this.draws = draws;
  }

  public void setTotalSeconds(int totalSeconds) {
    this.totalSeconds = totalSeconds;
  }

  public static class Builder {
    private Long id;
    private String lichessId;
    private String username;
    private String description;
    private String title;
    private LocalDateTime createdAt;
    private String bannerImage;
    private String profileImage;
    private LocalDateTime lichessCreatedAt;
    private LocalDateTime lastLoginAt;
    private int allGames;
    private int ratedGames;
    private int wins;
    private int losses;
    private int draws;
    private int totalSeconds;

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

    public Builder bannerImage(String bannerImage) {
      this.bannerImage = bannerImage;
      return this;
    }

    public Builder profileImage(String profileImage) {
      this.profileImage = profileImage;
      return this;
    }

    public Builder lichessCreatedAt(LocalDateTime lichessCreatedAt) {
      this.lichessCreatedAt = lichessCreatedAt;
      return this;
    }

    public Builder lastLoginAt(LocalDateTime lastLoginAt) {
      this.lastLoginAt = lastLoginAt;
      return this;
    }

    public Builder allGames(int allGames) {
      this.allGames = allGames;
      return this;
    }

    public Builder ratedGames(int ratedGames) {
      this.ratedGames = ratedGames;
      return this;
    }

    public Builder wins(int wins) {
      this.wins = wins;
      return this;
    }

    public Builder losses(int losses) {
      this.losses = losses;
      return this;
    }

    public Builder draws(int draws) {
      this.draws = draws;
      return this;
    }

    public Builder totalSeconds(int totalSeconds) {
      this.totalSeconds = totalSeconds;
      return this;
    }

    public LichessUser build() {
      return new LichessUser(this);
    }
  }
}

