package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;

public class UserPerfStat {

    private Long id;
    private Long userId;
    private OAuthPlatForm platform;
    private String timeClass;
    private int rating;
    private int games;
    private int wins;
    private int losses;
    private int draws;

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OAuthPlatForm getPlatform() { return platform; }
    public String getTimeClass() { return timeClass; }
    public int getRating() { return rating; }
    public int getGames() { return games; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }

    public void setId(Long id) { this.id = id; }

    public static class Builder {
        private Long id;
        private Long userId;
        private OAuthPlatForm platform;
        private String timeClass;
        private int rating;
        private int games;
        private int wins;
        private int losses;
        private int draws;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder platform(OAuthPlatForm platform) { this.platform = platform; return this; }
        public Builder timeClass(String timeClass) { this.timeClass = timeClass; return this; }
        public Builder rating(int rating) { this.rating = rating; return this; }
        public Builder games(int games) { this.games = games; return this; }
        public Builder wins(int wins) { this.wins = wins; return this; }
        public Builder losses(int losses) { this.losses = losses; return this; }
        public Builder draws(int draws) { this.draws = draws; return this; }

        public UserPerfStat build() {
            UserPerfStat s = new UserPerfStat();
            s.id = this.id;
            s.userId = this.userId;
            s.platform = this.platform;
            s.timeClass = this.timeClass;
            s.rating = this.rating;
            s.games = this.games;
            s.wins = this.wins;
            s.losses = this.losses;
            s.draws = this.draws;
            return s;
        }
    }
}
