package backend.chessmate.api.user.dto;

import backend.chessmate.api.user.entity.Streak;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class StreakDto {

    private LocalDate date;
    private int win;
    private int lose;
    private int draw;

    public void from(Streak streak) {
        this.date = streak.getDate();
        this.win = streak.getWin();
        this.lose = streak.getLose();
        this.draw = streak.getDraw();
    }
}
