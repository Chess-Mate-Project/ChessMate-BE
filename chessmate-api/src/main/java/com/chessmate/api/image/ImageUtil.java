package com.chessmate.api.image;

import com.chessmate.api.image.config.CloudflareProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ImageUtil {

  private final CloudflareProperties cloudflareProperties;

  public String getProfileImageUrl(Long userId, String profileKey) {
    return buildImageUrl(userId, profileKey, "profile");
  }

  public String getBannerImageUrl(Long userId, String bannerKey) {
    return buildImageUrl(userId, bannerKey, "banner");
  }

  private String buildImageUrl(Long userId, String key, String type) {
    String baseUrl = cloudflareProperties.getCdn();

    if (key == null || key.isEmpty()) {
      String defaultUrl = baseUrl + "/default/default_" + type + ".png";
      log.debug("이미지 없음 - userId={}, type={}, 기본 이미지 반환: {}", userId, type, defaultUrl);
      return defaultUrl;
    }

    return baseUrl + "/" + key;
  }
}
