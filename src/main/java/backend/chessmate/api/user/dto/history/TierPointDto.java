package backend.chessmate.api.user.dto.history;

import backend.chessmate.api.user.dto.TierResult;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TierPointDto {
    private LocalDate date;
    private TierResult tier;
}

