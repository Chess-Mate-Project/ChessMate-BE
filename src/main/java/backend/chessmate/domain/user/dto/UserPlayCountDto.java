package backend.chessmate.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserPlayCountDto {
    private int all;
    private int win;
    private int lose;
    private int draw;
}
