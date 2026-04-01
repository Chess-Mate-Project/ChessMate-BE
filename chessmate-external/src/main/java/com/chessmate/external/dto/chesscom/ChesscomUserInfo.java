package com.chessmate.external.dto.chesscom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Chess.com OAuth2 ID Token 정보
 *
 * Chess.com OpenID Connect의 ID Token 클레임을 매핑하는 클래스입니다.
 * JWT 토큰의 페이로드에서 사용자 정보를 추출합니다.
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChesscomUserInfo {

  /** 고유 식별자 (UUID format: 1f73c070-4b55-11ef-a472-1d7e14c696e5) */
  private String sub;

  /** 발행자 (항상 "https://oauth.chess.com") */
  private String iss;

  /** 토큰 발급 대상 (클라이언트 ID) */
  private String aud;

  /** 토큰 발급 시간 (Unix timestamp) */
  private Long iat;

  /** 토큰 만료 시간 (Unix timestamp) */
  private Long exp;

  /** Chess.com 사용자명 (wjdansrud0922) */
  @JsonProperty("preferred_username")
  private String username;

  /** 사용자 프로필 URL */
  private String profile;

  /** 프로필 이미지 URL */
  private String picture;

  /** 사용자의 시간대 */
  private String zoneinfo;

  /** 사용자의 로케일 (ko_KR) */
  private String locale;

  /** 숫자 형식의 사용자 ID (377471899) */
  @JsonProperty("user_id")
  private String userId;

  /** 국가명 (South Korea) */
  private String country;

  /** 국가 코드 (KR) */
  @JsonProperty("country_code")
  private String countryCode;

  /** 멤버십 등급 (basic, premium, mod, staff 등) */
  private String membership;

  /** 이메일 주소 */
  private String email;

  /** 이메일 인증 여부 */
  @JsonProperty("email_verified")
  private Boolean emailVerified;
}
