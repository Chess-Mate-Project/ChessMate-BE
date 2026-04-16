package com.chessmate.api.image.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudflare.r2")
public class CloudflareProperties {
  private String endpoint;
  private String accessKey;
  private String secretKey;
  private String bucket;
  private String cdn;
}