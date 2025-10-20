package backend.chessmate.domain.user.dto;

import backend.chessmate.domain.user.entity.type.SubTierType;
import backend.chessmate.domain.user.entity.type.TierType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TierResult {
    private TierType tier;
    private SubTierType subTier;
    private int rating;
}