package com.chessmate.infra_persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "users")
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

  @Column(name = "title")
  private String title;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;
}
