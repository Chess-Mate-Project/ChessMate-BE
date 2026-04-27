package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;

public class UserMonthlyRatingStat {

    private Long id;
    private Long userId;
    private OAuthPlatForm platform;
    private String timeClass;
    private int year;
    private int month;
    private int rating;

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OAuthPlatForm getPlatform() { return platform; }
    public String getTimeClass() { return timeClass; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getRating() { return rating; }
    public void setId(Long id) { this.id = id; }

    public static class Builder {
        private Long id;
        private Long userId;
        private OAuthPlatForm platform;
        private String timeClass;
        private int year;
        private int month;
        private int rating;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder platform(OAuthPlatForm platform) { this.platform = platform; return this; }
        public Builder timeClass(String timeClass) { this.timeClass = timeClass; return this; }
        public Builder year(int year) { this.year = year; return this; }
        public Builder month(int month) { this.month = month; return this; }
        public Builder rating(int rating) { this.rating = rating; return this; }

        public UserMonthlyRatingStat build() {
            UserMonthlyRatingStat s = new UserMonthlyRatingStat();
            s.id = this.id;
            s.userId = this.userId;
            s.platform = this.platform;
            s.timeClass = this.timeClass;
            s.year = this.year;
            s.month = this.month;
            s.rating = this.rating;
            return s;
        }
    }
}