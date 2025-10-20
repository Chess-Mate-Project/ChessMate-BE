package backend.chessmate.domain.user.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class TierInfoDto {
    TierResult classical;
    TierResult rapid;
    TierResult bullet;
    TierResult blitz;
}
