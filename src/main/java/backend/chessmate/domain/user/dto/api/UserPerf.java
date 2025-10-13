package backend.chessmate.domain.user.dto.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UserPerf {

    private User user;
    private Perf perf;
    private String rank;
    private Double percentile;
    private Stat stat;

    @Data
    public static class User {
        private String name;
    }

    @Data
    public static class Perf {
        private Glicko glicko;
        private int nb; // total games
        private int progress;

        @Data
        public static class Glicko {
            private double rating;
            private double deviation;
        }
    }

    @Data
    public static class Stat {
        private String id;
        private UserId userId;
        private PerfType perfType;
        private RatingRecord highest;
        private RatingRecord lowest;
        private BestWins bestWins;
        private WorstLosses worstLosses;
        private Count count;
        private ResultStreak resultStreak;
        private PlayStreak playStreak;

        @Data
        public static class UserId {
            private String id;
            private String name;
            private String title;
        }

        @Data
        public static class PerfType {
            private String key;
            private String name;
        }

        @Data
        public static class RatingRecord {
            @JsonProperty("int")
            private int rating;
            private String at;
            private String gameId;
        }

        @Data
        public static class BestWins {
            private List<WinLossRecord> results;
        }

        @Data
        public static class WorstLosses {
            private List<WinLossRecord> results;
        }

        @Data
        public static class WinLossRecord {
            private int opRating;
            private Opponent opId;
            private String at;
            private String gameId;

            @Data
            public static class Opponent {
                private String id;
                private String name;
                private String title;
            }
        }

        @Data
        public static class Count {
            private int all;
            private int rated;
            private int win;
            private int loss;
            private int draw;
            private int tour;
            private int berserk;
            private double opAvg;
            private long seconds;
            private int disconnects;
        }

        @Data
        public static class ResultStreak {
            private Streak win;
            private Streak loss;

            @Data
            public static class Streak {
                private CurrentMax cur;
                private CurrentMax max;

                @Data
                public static class CurrentMax {
                    @JsonProperty("v")
                    private int value;
                    private StreakFromTo from;
                    private StreakFromTo to;

                    @Data
                    public static class StreakFromTo {
                        private String at;
                        private String gameId;
                    }
                }
            }
        }

        @Data
        public static class PlayStreak {
            private Nb nb;
            private Time time;
            private String lastDate;

            @Data
            public static class Nb {
                private CurrentMax cur;
                private CurrentMax max;

                @Data
                public static class CurrentMax {
                    @JsonProperty("v")
                    private int value;
                    private StreakFromTo from;
                    private StreakFromTo to;

                    @Data
                    public static class StreakFromTo {
                        private String at;
                        private String gameId;
                    }
                }
            }

            @Data
            public static class Time {
                private CurrentMax cur;
                private CurrentMax max;

                @Data
                public static class CurrentMax {
                    @JsonProperty("v")
                    private int value;
                    private StreakFromTo from;
                    private StreakFromTo to;

                    @Data
                    public static class StreakFromTo {
                        private String at;
                        private String gameId;
                    }
                }
            }
        }
    }
}
