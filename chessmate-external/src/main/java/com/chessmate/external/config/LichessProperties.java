package com.chessmate.external.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Lichess OAuth 및 API 설정 정보를 담는 프로퍼티 클래스
 * application-external.yml의 oauth.lichess 섹션과 매핑됩니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth.lichess")
public class LichessProperties {

  /** Lichess Client ID */
  private String clientId;
  
  /** Lichess OAuth 리다이렉트 URI */
  private String redirectUri;

  private String oauthUrl;
  /** Lichess API 기본 URL */
  private String baseApiUrl;
}

