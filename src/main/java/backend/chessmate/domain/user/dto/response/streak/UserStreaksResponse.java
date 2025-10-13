package backend.chessmate.domain.user.dto.response.streak;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class UserStreaksResponse {
    private List<UserStreak> streaks;
}
