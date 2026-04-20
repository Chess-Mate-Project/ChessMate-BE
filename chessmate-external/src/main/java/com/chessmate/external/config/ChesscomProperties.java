package com.chessmate.external.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Chess.com OAuth 및 API 설정 정보를 담는 프로퍼티 클래스
 * application-external.yml의 oauth.chesscom 섹션과 매핑됩니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth.chesscom")
public class ChesscomProperties {

  /** Chess.com OAuth 클라이언트 ID */
  private String clientId;

  /** Chess.com OAuth 인증 URL */
  private String oauthUrl;

  /** Chess.com OAuth 리다이렉트 URI */
  private String redirectUri;

  /** Chess.com API 기본 URL */
  private String baseApiUrl;
}

