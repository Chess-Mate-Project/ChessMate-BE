package backend.chessmate.domain.user.dto.response.streak;

import backend.chessmate.domain.user.entity.Streak;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class UserStreak {
    private LocalDate date;
    private int total;
    private int win;
    private int lose;
    private int draw;
    private String eco;
    private String opening;

}
