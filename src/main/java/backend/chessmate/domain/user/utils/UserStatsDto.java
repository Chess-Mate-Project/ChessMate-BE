package backend.chessmate.domain.user.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserStatsDto {
    private Map<LocalDate, Status> statusByDate;
    private Map<String, Long> countByOpening;
    private Map<String, Long> countByFirstMove;
}
