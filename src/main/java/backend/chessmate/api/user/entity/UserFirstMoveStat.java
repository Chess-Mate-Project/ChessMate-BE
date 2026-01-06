package backend.chessmate.api.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_first_move_stat")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class UserFirstMoveStat {

  @Id @GeneratedValue
  private Long id;

  private Long userId;

  private String firstMove; // e4, d4 등
}
