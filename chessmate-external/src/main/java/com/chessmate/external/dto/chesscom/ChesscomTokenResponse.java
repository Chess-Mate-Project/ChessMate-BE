package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Chess.com OAuth 토큰 응답 DTO
 * Chess.com의 /token 엔드포인트 응답을 매핑
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChesscomTokenResponse {

  /**
   * ID Token (JWT 형식)
   * 사용자 정보를 포함한 JWT 토큰
   */
  @JsonProperty("id_token")
  private String idToken;

  /**
   * 토큰 유형 (Bearer)
   */
  @JsonProperty("token_type")
  private String tokenType;

  /**
   * Access Token 만료 시간 (초 단위)
   * 기본값: 86400초 (24시간)
   */
  @JsonProperty("expires_in")
  private Integer expiresIn;

  /**
   * Access Token (JWT 형식)
   * API 요청 시 Authorization 헤더에 사용
   */
  @JsonProperty("access_token")
  private String accessToken;

  /**
   * Refresh Token
   * Access Token 만료 후 새로운 Token을 발급받을 때 사용
   */
  @JsonProperty("refresh_token")
  private String refreshToken;

}

