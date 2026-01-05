package backend.chessmate.api.user.dto.mapper;

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
