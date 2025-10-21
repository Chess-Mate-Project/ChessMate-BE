package backend.chessmate.domain.user.dto.history;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTierHistoryDto {
    List<TierPointDto> classicalHistory;
    List<TierPointDto> rapidHistory;
    List<TierPointDto> bulletHistory;
    List<TierPointDto> blitzHistory;
}
