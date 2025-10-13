package backend.chessmate.domain.user.dto.response.tier;

import backend.chessmate.domain.user.entity.type.GameType;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@Builder
public class TierResponse {
    private String userName;
    private GameType gameType;
    private TierResult result;

}
