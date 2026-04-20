package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chess.com Refresh Token 요청 DTO
 * 새로운 Access Token을 발급받기 위한 요청
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChesscomRefreshTokenRequest {

  /**
   * Grant Type: refresh_token (고정값)
   */
  @JsonProperty("grant_type")
  private String grantType;

  /**
   * Refresh Token
   * 이전에 받았던 Refresh Token
   */
  @JsonProperty("refresh_token")
  private String refreshToken;

  /**
   * Client ID
   * Chess.com 애플리케이션 ID
   */
  @JsonProperty("client_id")
  private String clientId;

  // 팩토리 메서드
  public static ChesscomRefreshTokenRequest of(String refreshToken, String clientId) {
    return ChesscomRefreshTokenRequest.builder()
        .grantType("refresh_token")
        .refreshToken(refreshToken)
        .clientId(clientId)
        .build();
  }
}

