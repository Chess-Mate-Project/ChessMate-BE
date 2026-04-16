package com.chessmate.domain.stat;

import com.chessmate.common.dto.OAuthPlatForm;

public class UserFirstMoveStat {

    private Long id;
    private Long userId;
    private OAuthPlatForm platform;
    private String timeClass;
    private String color;   // "WHITE" or "BLACK"
    private String move;    // e.g. "e4", "d4", "Nf3"
    private int count;

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OAuthPlatForm getPlatform() { return platform; }
    public String getTimeClass() { return timeClass; }
    public String getColor() { return color; }
    public String getMove() { return move; }
    public int getCount() { return count; }

    public void setId(Long id) { this.id = id; }

    public static class Builder {
        private Long id;
        private Long userId;
        private OAuthPlatForm platform;
        private String timeClass;
        private String color;
        private String move;
        private int count;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder platform(OAuthPlatForm platform) { this.platform = platform; return this; }
        public Builder timeClass(String timeClass) { this.timeClass = timeClass; return this; }
        public Builder color(String color) { this.color = color; return this; }
        public Builder move(String move) { this.move = move; return this; }
        public Builder count(int count) { this.count = count; return this; }

        public UserFirstMoveStat build() {
            UserFirstMoveStat s = new UserFirstMoveStat();
            s.id = this.id;
            s.userId = this.userId;
            s.platform = this.platform;
            s.timeClass = this.timeClass;
            s.color = this.color;
            s.move = this.move;
            s.count = this.count;
            return s;
        }
    }
}