package com.chessmate.external.dto.lichess;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Lichess OAuth 토큰 응답 DTO
 * Lichess의 /api/token 엔드포인트 응답을 매핑
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichessTokenResponse {

  /**
   * Access Token
   * API 요청 시 Authorization 헤더에 사용
   */
  @JsonProperty("access_token")
  private String accessToken;

  /**
   * 토큰 유형 (Bearer)
   */
  @JsonProperty("token_type")
  private String tokenType;

  /**
   * Access Token 만료 시간 (초 단위)
   * Lichess는 보통 2개월 정도
   */
  @JsonProperty("expires_in")
  private Integer expiresIn;

}

