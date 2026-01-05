package backend.chessmate.api.user.dto.history;

import backend.chessmate.api.user.entity.type.GameType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTierHistoryDto {
    private GameType gameType;
    private List<TierPointDto> history;
}
