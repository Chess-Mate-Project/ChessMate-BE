package backend.chessmate.domain.user.dto.mapper;

import backend.chessmate.domain.user.dto.history.TierPointDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRatingHistoryMapper {
    private List<TierPointDto> bulletHistory;
    private List<TierPointDto> blitzHistory;
    private List<TierPointDto> rapidHistory;
    private List<TierPointDto> classicalHistory;
}
