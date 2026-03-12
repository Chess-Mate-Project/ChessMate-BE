package com.chessmate.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ChesscomTokenResponse {
  private String accessToken;
  private String idToken;
  private String tokenType;
  private Long expiresIn;
  private String refreshToken;
}
