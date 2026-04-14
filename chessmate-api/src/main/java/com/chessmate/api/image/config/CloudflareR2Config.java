package com.chessmate.api.image.config;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(CloudflareProperties.class)
@RequiredArgsConstructor
public class CloudflareR2Config {

  private final CloudflareProperties cloudflareProperties;

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(cloudflareProperties.getEndpoint()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    cloudflareProperties.getAccessKey(),
                    cloudflareProperties.getSecretKey()
                )
            )
        )
        .region(Region.US_EAST_1)
        .httpClient(ApacheHttpClient.builder().build())
        .build();
  }

  @Bean
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .endpointOverride(URI.create(cloudflareProperties.getEndpoint()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    cloudflareProperties.getAccessKey(),
                    cloudflareProperties.getSecretKey()
                )
            )
        )
        .region(Region.US_EAST_1)
        .build();
  }
}
