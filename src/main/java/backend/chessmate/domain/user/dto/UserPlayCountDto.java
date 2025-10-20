package backend.chessmate.domain.user.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserPlayCountDto {
    private int all;
    private int win;
    private int lose;
    private int draw;
}
