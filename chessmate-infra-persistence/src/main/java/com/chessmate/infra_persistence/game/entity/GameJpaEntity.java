package com.chessmate.infra_persistence.game.entity;

import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.domain.game.GameResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "game",
    indexes = {
        @Index(name = "idx_game_user_platform", columnList = "user_id, platform")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_game_user_platform_id", columnNames = {"user_id", "platform", "platform_game_id"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private OAuthPlatForm platform;

    @Column(name = "platform_game_id", nullable = false, length = 255)
    private String platformGameId;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "opponent_username", length = 100)
    private String opponentUsername;

    @Column(name = "player_color", length = 10)
    private String playerColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 10)
    private GameResult result;

    @Column(name = "time_class", length = 20)
    private String timeClass;

    @Column(name = "time_control", length = 30)
    private String timeControl;

    @Column(name = "rated")
    private Boolean rated;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "moves", columnDefinition = "TEXT")
    private String moves;

    @Column(name = "variant", length = 30)
    private String variant;

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}