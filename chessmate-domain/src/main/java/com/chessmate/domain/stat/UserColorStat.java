package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;

public class UserColorStat {

    private Long id;
    private Long userId;
    private OAuthPlatForm platform;
    private String timeClass;
    private String color;   // "WHITE" or "BLACK"
    private int wins;
    private int draws;
    private int losses;

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OAuthPlatForm getPlatform() { return platform; }
    public String getTimeClass() { return timeClass; }
    public String getColor() { return color; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }
    public int getTotal() { return wins + draws + losses; }

    public void setId(Long id) { this.id = id; }

    public static class Builder {
        private Long id;
        private Long userId;
        private OAuthPlatForm platform;
        private String timeClass;
        private String color;
        private int wins;
        private int draws;
        private int losses;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder platform(OAuthPlatForm platform) { this.platform = platform; return this; }
        public Builder timeClass(String timeClass) { this.timeClass = timeClass; return this; }
        public Builder color(String color) { this.color = color; return this; }
        public Builder wins(int wins) { this.wins = wins; return this; }
        public Builder draws(int draws) { this.draws = draws; return this; }
        public Builder losses(int losses) { this.losses = losses; return this; }

        public UserColorStat build() {
            UserColorStat s = new UserColorStat();
            s.id = this.id;
            s.userId = this.userId;
            s.platform = this.platform;
            s.timeClass = this.timeClass;
            s.color = this.color;
            s.wins = this.wins;
            s.draws = this.draws;
            s.losses = this.losses;
            return s;
        }
    }
}