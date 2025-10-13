package backend.chessmate.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GameSummaryDto {
    private int classical;
    private int rapid;
    private int bullet;
    private int blitz;
}
