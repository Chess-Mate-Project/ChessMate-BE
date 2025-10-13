package backend.chessmate.domain.user.dto.api;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserAccount {
    private String id;
    private String username;
    private Perfs perfs;
    private long createdAt;
    private long seenAt;
    private PlayTime playTime;
    private String url;
    private Count count;
    private boolean followable;
    private boolean following;
    private boolean blocking;

    @Data
    public static class Perfs {
        private Perf bullet;
        private Perf blitz;
        private Perf rapid;
        private Perf classical;
        private Perf correspondence;
    }

    @Data
    public static class Perf {
        private int games;
        private int rating;
        private int rd;
        private int prog;
        private boolean prov;
    }

    @Data
    public static class PlayTime {
        private int total;
        private int tv;
    }

    @Data
    public static class Count {
        private int all;
        private int rated;
        private int ai;
        private int draw;
        private int drawH;
        private int loss;
        private int lossH;
        private int win;
        private int winH;
        private int bookmark;
        private int playing;

        @JsonProperty("import")
        private int importCount;

        private int me;
    }
}
