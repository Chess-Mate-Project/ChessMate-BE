package com.chessmate.infra_core.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "lichess_profile")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class LichessProfile extends Profile {

  @Column(name = "lichess_id", nullable = false, unique = true)
  private String lichessId;

  @Override
  public OauthPlatForm getProvider() {
    return OauthPlatForm.LICHESS;
  }
}

