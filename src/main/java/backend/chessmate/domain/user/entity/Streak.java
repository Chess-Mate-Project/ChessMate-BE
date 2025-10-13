package backend.chessmate.domain.user.entity;

import backend.chessmate.domain.auth.entity.User;
import backend.chessmate.domain.user.entity.key.StreakId;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;


@Entity
@Table(name = "streak")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@IdClass(StreakId.class)
public class Streak {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 유저와 매핑 추가

    @Id
    private LocalDate date;

    @Column(name = "win", nullable = false)
    private Integer win; // 해당 날짜에 사용자가 이긴 게임 수

    @Column(name = "lose", nullable = false)
    private Integer lose; // 해당 날짜에 사용자가 진 게임 수

    @Column(name = "draw", nullable = false)
    private Integer draw; // 해당 날짜에 사용자가 비긴 게임 수

    @Column(name = "last_move_at", nullable = false)
    private Long lastMoveAt; // 마지막 수가 둬진 시간 (timestamp in milliseconds) - 마지막으로 업데이트 된 게임 시간
}