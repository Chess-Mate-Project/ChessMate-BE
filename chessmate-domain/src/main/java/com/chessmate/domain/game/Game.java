package com.chessmate.domain.game;

import com.chessmate.common.dto.OAuthPlatForm;
import java.time.LocalDateTime;

public class Game {

    private Long id;
    private Long userId;
    private OAuthPlatForm platform;
    private String platformGameId;
    private String username;
    private String opponentUsername;
    private String playerColor;   // "WHITE" or "BLACK"
    private GameResult result;
    private String timeClass;
    private String timeControl;
    private Boolean rated;
    private Integer rating;
    private String moves;
    private String variant;
    private LocalDateTime playedAt;
    private LocalDateTime createdAt;

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OAuthPlatForm getPlatform() { return platform; }
    public String getPlatformGameId() { return platformGameId; }
    public String getUsername() { return username; }
    public String getOpponentUsername() { return opponentUsername; }
    public String getPlayerColor() { return playerColor; }
    public GameResult getResult() { return result; }
    public String getTimeClass() { return timeClass; }
    public String getTimeControl() { return timeControl; }
    public Boolean getRated() { return rated; }
    public Integer getRating() { return rating; }
    public String getMoves() { return moves; }
    public String getVariant() { return variant; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setPlatform(OAuthPlatForm platform) { this.platform = platform; }
    public void setPlatformGameId(String platformGameId) { this.platformGameId = platformGameId; }
    public void setUsername(String username) { this.username = username; }
    public void setOpponentUsername(String opponentUsername) { this.opponentUsername = opponentUsername; }
    public void setPlayerColor(String playerColor) { this.playerColor = playerColor; }
    public void setResult(GameResult result) { this.result = result; }
    public void setTimeClass(String timeClass) { this.timeClass = timeClass; }
    public void setTimeControl(String timeControl) { this.timeControl = timeControl; }
    public void setRated(Boolean rated) { this.rated = rated; }
    public void setRating(Integer rating) { this.rating = rating; }
    public void setMoves(String moves) { this.moves = moves; }
    public void setVariant(String variant) { this.variant = variant; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class Builder {
        private Long id;
        private Long userId;
        private OAuthPlatForm platform;
        private String platformGameId;
        private String username;
        private String opponentUsername;
        private String playerColor;
        private GameResult result;
        private String timeClass;
        private String timeControl;
        private Boolean rated;
        private Integer rating;
        private String moves;
        private String variant;
        private LocalDateTime playedAt;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder userId(Long userId) { this.userId = userId; return this; }
        public Builder platform(OAuthPlatForm platform) { this.platform = platform; return this; }
        public Builder platformGameId(String platformGameId) { this.platformGameId = platformGameId; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder opponentUsername(String opponentUsername) { this.opponentUsername = opponentUsername; return this; }
        public Builder playerColor(String playerColor) { this.playerColor = playerColor; return this; }
        public Builder result(GameResult result) { this.result = result; return this; }
        public Builder timeClass(String timeClass) { this.timeClass = timeClass; return this; }
        public Builder timeControl(String timeControl) { this.timeControl = timeControl; return this; }
        public Builder rated(Boolean rated) { this.rated = rated; return this; }
        public Builder rating(Integer rating) { this.rating = rating; return this; }
        public Builder moves(String moves) { this.moves = moves; return this; }
        public Builder variant(String variant) { this.variant = variant; return this; }
        public Builder playedAt(LocalDateTime playedAt) { this.playedAt = playedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Game build() {
            Game game = new Game();
            game.id = this.id;
            game.userId = this.userId;
            game.platform = this.platform;
            game.platformGameId = this.platformGameId;
            game.username = this.username;
            game.opponentUsername = this.opponentUsername;
            game.playerColor = this.playerColor;
            game.result = this.result;
            game.timeClass = this.timeClass;
            game.timeControl = this.timeControl;
            game.rated = this.rated;
            game.rating = this.rating;
            game.moves = this.moves;
            game.variant = this.variant;
            game.playedAt = this.playedAt;
            game.createdAt = this.createdAt;
            return game;
        }
    }
}