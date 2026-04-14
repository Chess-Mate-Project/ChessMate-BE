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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "user_perf_stat",
    indexes = {
        @Index(name = "idx_perf_stat_user_platform", columnList = "user_id, platform")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_perf_stat", columnNames = {"user_id", "platform", "time_class"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPerfStatJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private OAuthPlatForm platform;

    @Column(name = "time_class", nullable = false, length = 20)
    private String timeClass;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "games", nullable = false)
    private int games;

    @Column(name = "wins", nullable = false)
    private int wins;

    @Column(name = "losses", nullable = false)
    private int losses;

    @Column(name = "draws", nullable = false)
    private int draws;
}
