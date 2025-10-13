package backend.chessmate.domain.user.dto.api;

import lombok.Data;

@Data
public class UserGame {
    private String id;
    private boolean rated;
    private String variant;
    private String speed;
    private String perf;
    private long createdAt;
    private long lastMoveAt;
    private String status;
    private String source;
    private Players players;
    private String winner;
    private Opening opening;
    private String moves;
    private Clock clock;

    @Data
    public static class Players {
        private PlayerInfo white;
        private PlayerInfo black;
    }

    @Data
    public static class PlayerInfo {
        private User user;
        private int rating;
        private int ratingDiff;
    }


    @Data
    public static class User {
        private String name;
        private String id;
    }

    @Data
    public static class Opening {
        private String eco;
        private String name;
        private int ply;
    }

    @Data
    public static class Clock {
        private int initial;
        private int increment;
        private int totalTime;
    }
}