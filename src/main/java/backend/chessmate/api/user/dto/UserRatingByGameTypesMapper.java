package backend.chessmate.api.user.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRatingByGameTypesMapper {
    private int classicalRating;
    private int rapidRating;
    private int bulletRating;
    private int blitzRating;
}
