package backend.chessmate.api.user.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Status {
    private int win;
    private int lose;
    private int draw;
    private long lastMoveAt;

    public void add(int win, int lose, int draw, long lastMoveAt) {
        this.win += win;
        this.lose += lose;
        this.draw += draw;
        this.lastMoveAt = Math.max(lastMoveAt, this.lastMoveAt);

    }
}
