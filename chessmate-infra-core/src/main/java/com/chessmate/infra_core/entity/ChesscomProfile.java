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
@DiscriminatorValue("CHESSCOM") // platform_type 컬럼에 "LICHESS"라고 저장됨
@Getter@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChesscomProfile extends Profile {

  @Column(name = "chesscom_id", nullable = false, unique = true)
  private Integer chesscomId; // PK (Integer)

  @Override
  public OauthPlatForm getProvider() {
    return OauthPlatForm.CHESSCOM;
  }
}
