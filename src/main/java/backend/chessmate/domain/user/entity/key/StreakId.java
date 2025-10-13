package backend.chessmate.domain.user.entity.key;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreakId implements Serializable {
    private Long user; // user ID
    private LocalDate date; // yyyy-mm-dd


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StreakId other = (StreakId) obj;
        return java.util.Objects.equals(user, other.user) &&
                java.util.Objects.equals(date, other.date);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(user, date);
    }

}
