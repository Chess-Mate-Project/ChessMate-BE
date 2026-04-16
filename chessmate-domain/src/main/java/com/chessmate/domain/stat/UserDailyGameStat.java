package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;
import java.time.LocalDate;

public class UserDailyGameStat {

    private Long id;
    private Long userId;
    private OAuthPlatForm platform;
    private LocalDate date;
    private int total;
    private int wins;
    private int draws;
    private int losses;

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OAuthPlatForm getPlatform() { return platform; }
    public LocalDate getDate() { return date; }
    public int getTotal() { return total; }
    public int getWins() { return wins; }
    public int getDraws() { return draws; }
    public int getLosses() { return losses; }

    public void setId(Long id) { this.id = id; }

    public static class Builder {
        private Long id;
        private Long userId;
        private OAuthPlatForm platform;
        private LocalDate date;
        private int total;
        private int wins;
        private int draws;
        private int losses;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder platform(OAuthPlatForm platform) { this.platform = platform; return this; }
        public Builder date(LocalDate date) { this.date = date; return this; }
        public Builder total(int total) { this.total = total; return this; }
        public Builder wins(int wins) { this.wins = wins; return this; }
        public Builder draws(int draws) { this.draws = draws; return this; }
        public Builder losses(int losses) { this.losses = losses; return this; }

        public UserDailyGameStat build() {
            UserDailyGameStat s = new UserDailyGameStat();
            s.id = this.id;
            s.userId = this.userId;
            s.platform = this.platform;
            s.date = this.date;
            s.total = this.total;
            s.wins = this.wins;
            s.draws = this.draws;
            s.losses = this.losses;
            return s;
        }
    }
}