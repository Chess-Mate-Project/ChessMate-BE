package com.chessmate.external.chesscom;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oauth.chesscom")
@Getter
@Setter
public class ChesscomConfig {

  private String clientId;
  private String oauthUrl;
  private String tokenUrl;
  private String redirectUrl;
  private String baseApiUrl;
}
