package backend.chessmate.api.user.entity;


import backend.chessmate.api.user.entity.key.FirstMoveId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "first_move")
@IdClass(FirstMoveId.class)
public class FirstMove {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    private String move;

    private Long count;


}
