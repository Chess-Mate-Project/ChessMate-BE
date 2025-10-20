package backend.chessmate.domain.user.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class GameSummaryDto {
    private int classical;
    private int rapid;
    private int bullet;
    private int blitz;
}
