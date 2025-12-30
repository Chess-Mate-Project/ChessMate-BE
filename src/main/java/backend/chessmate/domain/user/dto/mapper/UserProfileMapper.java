package backend.chessmate.domain.user.dto.mapper;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileMapper {
    private String flag;
    private String bio;
    private long playTime; //시간
    private String link;
}
