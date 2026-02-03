package com.chessmate.domain.userPerf;

import com.chessmate.common.type.GameType;

public class UserPerf {
  private Long userId;
  private GameType gameType;
  private int rating;
  private int gamesPlayed;
  private boolean prov;

  // 게임 통계
  private int all;
  private int rated;
  private int wins;
  private int losses;
  private int draws;
  private int tour;
  private int berserk;
  private double opAvg;
  private int seconds;
  private int disconnects;

  // 레이팅 관련
  private int highestRating;
  private int lowestRating;
  private int maxStreak;
  private int maxLossStreak;
  private boolean uncertain;

  // 무인자 생성자
  public UserPerf() {}

  private UserPerf(Builder builder) {
    this.userId = builder.userId;
    this.gameType = builder.gameType;
    this.rating = builder.rating;
    this.gamesPlayed = builder.gamesPlayed;
    this.prov = builder.prov;
    this.all = builder.all;
    this.rated = builder.rated;
    this.wins = builder.wins;
    this.losses = builder.losses;
    this.draws = builder.draws;
    this.tour = builder.tour;
    this.berserk = builder.berserk;
    this.opAvg = builder.opAvg;
    this.seconds = builder.seconds;
    this.disconnects = builder.disconnects;
    this.highestRating = builder.highestRating;
    this.lowestRating = builder.lowestRating;
    this.maxStreak = builder.maxStreak;
    this.maxLossStreak = builder.maxLossStreak;
    this.uncertain = builder.uncertain;
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters
  public Long getUserId() {
    return userId;
  }

  public GameType getGameType() {
    return gameType;
  }

  public int getRating() {
    return rating;
  }

  public int getGamesPlayed() {
    return gamesPlayed;
  }

  public boolean isProv() {
    return prov;
  }

  public int getAll() {
    return all;
  }

  public int getRated() {
    return rated;
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

  public int getTour() {
    return tour;
  }

  public int getBerserk() {
    return berserk;
  }

  public double getOpAvg() {
    return opAvg;
  }

  public int getSeconds() {
    return seconds;
  }

  public int getDisconnects() {
    return disconnects;
  }

  public int getHighestRating() {
    return highestRating;
  }

  public int getLowestRating() {
    return lowestRating;
  }

  public int getMaxStreak() {
    return maxStreak;
  }

  public int getMaxLossStreak() {
    return maxLossStreak;
  }

  public boolean isUncertain() {
    return uncertain;
  }

  // Setters
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setGameType(GameType gameType) {
    this.gameType = gameType;
  }

  public void setRating(int rating) {
    this.rating = rating;
  }

  public void setGamesPlayed(int gamesPlayed) {
    this.gamesPlayed = gamesPlayed;
  }

  public void setProv(boolean prov) {
    this.prov = prov;
  }

  public void setAll(int all) {
    this.all = all;
  }

  public void setRated(int rated) {
    this.rated = rated;
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

  public void setTour(int tour) {
    this.tour = tour;
  }

  public void setBerserk(int berserk) {
    this.berserk = berserk;
  }

  public void setOpAvg(double opAvg) {
    this.opAvg = opAvg;
  }

  public void setSeconds(int seconds) {
    this.seconds = seconds;
  }

  public void setDisconnects(int disconnects) {
    this.disconnects = disconnects;
  }

  public void setHighestRating(int highestRating) {
    this.highestRating = highestRating;
  }

  public void setLowestRating(int lowestRating) {
    this.lowestRating = lowestRating;
  }

  public void setMaxStreak(int maxStreak) {
    this.maxStreak = maxStreak;
  }

  public void setMaxLossStreak(int maxLossStreak) {
    this.maxLossStreak = maxLossStreak;
  }

  public void setUncertain(boolean uncertain) {
    this.uncertain = uncertain;
  }

  public static class Builder {
    private Long userId;
    private GameType gameType;
    private int rating;
    private int gamesPlayed;
    private boolean prov;
    private int all;
    private int rated;
    private int wins;
    private int losses;
    private int draws;
    private int tour;
    private int berserk;
    private double opAvg;
    private int seconds;
    private int disconnects;
    private int highestRating;
    private int lowestRating;
    private int maxStreak;
    private int maxLossStreak;
    private boolean uncertain;

    public Builder userId(Long userId) {
      this.userId = userId;
      return this;
    }

    public Builder gameType(GameType gameType) {
      this.gameType = gameType;
      return this;
    }

    public Builder rating(int rating) {
      this.rating = rating;
      return this;
    }

    public Builder gamesPlayed(int gamesPlayed) {
      this.gamesPlayed = gamesPlayed;
      return this;
    }

    public Builder prov(boolean prov) {
      this.prov = prov;
      return this;
    }

    public Builder all(int all) {
      this.all = all;
      return this;
    }

    public Builder rated(int rated) {
      this.rated = rated;
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

    public Builder tour(int tour) {
      this.tour = tour;
      return this;
    }

    public Builder berserk(int berserk) {
      this.berserk = berserk;
      return this;
    }

    public Builder opAvg(double opAvg) {
      this.opAvg = opAvg;
      return this;
    }

    public Builder seconds(int seconds) {
      this.seconds = seconds;
      return this;
    }

    public Builder disconnects(int disconnects) {
      this.disconnects = disconnects;
      return this;
    }

    public Builder highestRating(int highestRating) {
      this.highestRating = highestRating;
      return this;
    }

    public Builder lowestRating(int lowestRating) {
      this.lowestRating = lowestRating;
      return this;
    }

    public Builder maxStreak(int maxStreak) {
      this.maxStreak = maxStreak;
      return this;
    }

    public Builder maxLossStreak(int maxLossStreak) {
      this.maxLossStreak = maxLossStreak;
      return this;
    }

    public Builder uncertain(boolean uncertain) {
      this.uncertain = uncertain;
      return this;
    }

    public UserPerf build() {
      return new UserPerf(this);
    }
  }
}

