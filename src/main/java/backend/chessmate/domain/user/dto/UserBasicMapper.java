package backend.chessmate.domain.user.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBasicMapper {
    private String lichessId;
    private String name;
    private LocalDate createdAt;
}
