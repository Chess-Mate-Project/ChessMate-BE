package com.chessmate.infra_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chesscom_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChesscomProfileEntity {
    @Id
    @Column(name = "player_id")
    private Integer playerId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column
    private String title;

    @Column
    private String status;

    @Column(nullable = false)
    private LocalDateTime joined;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

