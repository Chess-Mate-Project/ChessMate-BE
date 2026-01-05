package backend.chessmate.api.user.entity;


import backend.chessmate.api.user.entity.key.OpeningId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "opening")
@IdClass(OpeningId.class)
public class Opening {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    private String opening;

    private Long count;


}
