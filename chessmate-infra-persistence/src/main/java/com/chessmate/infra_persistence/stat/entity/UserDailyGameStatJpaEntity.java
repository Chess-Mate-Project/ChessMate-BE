package com.chessmate.infra_persistence.stat.entity;

import com.chessmate.common.dto.OAuthPlatForm;
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
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "user_daily_game_stat",
    indexes = {
        @Index(name = "idx_daily_stat_user_platform", columnList = "user_id, platform")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_stat", columnNames = {"user_id", "platform", "date"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyGameStatJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private OAuthPlatForm platform;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "total", nullable = false)
    private int total;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "draws", nullable = false)
    private int draws;

    @Column(name = "losses", nullable = false)
    private int losses;
}