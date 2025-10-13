package backend.chessmate.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GamesInUserInfo {
    private String opening;
    private int openingCount;
    private String firstMove;
    private int firstMoveCount;
}
