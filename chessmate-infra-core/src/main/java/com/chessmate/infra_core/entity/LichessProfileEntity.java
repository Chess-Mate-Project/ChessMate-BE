package com.chessmate.infra_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lichess_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichessProfileEntity {
    @Id
    @Column(name = "lichess_id")
    private String lichessId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column(name = "join_at")
    private LocalDateTime joinAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

