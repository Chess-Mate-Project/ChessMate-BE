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
    name = "user_first_move_stat",
    indexes = {
        @Index(name = "idx_first_move_user_platform", columnList = "user_id, platform")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_first_move_stat", columnNames = {"user_id", "platform", "time_class", "color", "move"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFirstMoveStatJpaEntity {

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

    @Column(name = "color", nullable = false, length = 10)
    private String color;

    @Column(name = "move", nullable = false, length = 10)
    private String move;

    @Column(name = "count", nullable = false)
    private int count;
}