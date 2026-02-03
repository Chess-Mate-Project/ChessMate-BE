package com.chessmate.infra_persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_last_login", columnList = "last_login_at")
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lichess_id", nullable = false, unique = true)
  private String lichessId;

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "description")
  private String description;

  @Column(name = "banner_image")
  private String bannerImage;

  @Column(name = "profile_image")
  private String profileImage;

  @Column(name = "title")
  private String title;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "lichess_created_at")
  private LocalDateTime lichessCreatedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "all_games", nullable = false)
  private int allGames;

  @Column(name = "rated_games", nullable = false)
  private int ratedGames;

  @Column(name = "wins", nullable = false)
  private int wins;

  @Column(name = "losses", nullable = false)
  private int losses;

  @Column(name = "draws", nullable = false)
  private int draws;

  @Column(name = "total_seconds", nullable = false)
  private int totalSeconds;
}
