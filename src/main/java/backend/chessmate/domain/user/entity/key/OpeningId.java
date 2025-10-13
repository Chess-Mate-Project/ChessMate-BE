package backend.chessmate.domain.user.entity.key;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpeningId implements Serializable {
    private Long user;
    private String opening;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OpeningId other = (OpeningId) obj;
        return java.util.Objects.equals(user, other.user) &&
                java.util.Objects.equals(opening, other.opening);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(user, opening);
    }
}
