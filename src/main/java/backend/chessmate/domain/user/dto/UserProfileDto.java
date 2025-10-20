package backend.chessmate.domain.user.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private String name;
    private String flag;
    private String bio;
    private LocalDate createAt; //계정 개설일
    private long playTime; //시간
    private String link;
}
