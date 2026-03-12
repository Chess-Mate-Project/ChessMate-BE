package com.chessmate.api.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LichessTokenResponse {
  private String accessToken;
  private int expiresIn;
  private String tokenType;
}
