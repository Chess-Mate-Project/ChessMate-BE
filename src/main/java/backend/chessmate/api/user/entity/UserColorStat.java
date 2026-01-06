  package backend.chessmate.api.user.entity;

  import backend.chessmate.api.user.entity.type.ChessColor;
  import backend.chessmate.api.user.entity.type.GameResult;
  import jakarta.persistence.Entity;
  import jakarta.persistence.EnumType;
  import jakarta.persistence.Enumerated;
  import jakarta.persistence.GeneratedValue;
  import jakarta.persistence.Id;
  import jakarta.persistence.Table;
  import lombok.AllArgsConstructor;
  import lombok.Builder;
  import lombok.NoArgsConstructor;
  import lombok.Setter;

  @Entity
  @Table(name = "user_color_stat")
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Setter
  public class UserColorStat {

    @Id
    @GeneratedValue
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private ChessColor color; // BLACK / WHITE

    @Enumerated(EnumType.STRING)
    private GameResult result; // WIN / LOSE / DRAW
  }
