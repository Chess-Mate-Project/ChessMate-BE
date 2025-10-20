package backend.chessmate.domain.auth.entity;

import backend.chessmate.domain.user.entity.type.BannerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lichess_id", nullable = false, unique = true)
    private String lichessId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Long lastSyncedAt; //update 전용 동기화 시간


    @PrePersist
    public void prePersist() {
        this.role = Role.USER; // 기본 역할은 USER로 설정
        this.lastSyncedAt =  System.currentTimeMillis();
    }


}
