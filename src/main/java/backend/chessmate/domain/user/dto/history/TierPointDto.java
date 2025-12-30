package backend.chessmate.domain.user.dto.history;

import backend.chessmate.domain.user.dto.TierResult;
import backend.chessmate.domain.user.entity.type.SubTierType;
import backend.chessmate.domain.user.entity.type.TierType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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

