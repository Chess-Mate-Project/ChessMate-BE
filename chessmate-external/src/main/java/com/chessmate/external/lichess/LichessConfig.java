package com.chessmate.external.lichess;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oauth.lichess")
@Getter
@Setter
public class LichessConfig {

  private String clientId;
  private String oauthUrl;
  private String tokenUrl;
  private String redirectUrl;
  private String baseApiUrl;
}
