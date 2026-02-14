package com.chessmate.api.image;

import com.chessmate.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ImageUtil {

  private final CloudflareProperties cloudflareProperties;

  public String getProfileImageUrl(User u) {
    log.debug("프로필 이미지 URL 조회 - userId={}", u.getId());

    if (cloudflareProperties == null) {
      log.warn("CloudflareProperties가 null입니다. 기본값 반환");
      return "/default/default_profile.png";
    }

    String key = u.getProfileImage();
    String baseUrl = cloudflareProperties.getCdn();

    log.debug("프로필 이미지 key={}, baseUrl={}", key, baseUrl);

    if (key == null || key.isEmpty()) {
      String defaultUrl = baseUrl + "/default/default_profile.png";
      log.info("프로필 이미지 없음 - userId={}, 기본 이미지 반환: {}", u.getId(), defaultUrl);
      return defaultUrl;
    }

    String imageUrl = baseUrl + "/" + key;
    log.debug("프로필 이미지 URL 생성 - userId={}, url={}", u.getId(), imageUrl);
    return imageUrl;
  }

  public String getBannerImageUrl(User u) {
    log.debug("배너 이미지 URL 조회 - userId={}", u.getId());

    if (cloudflareProperties == null) {
      log.warn("CloudflareProperties가 null입니다. 기본값 반환");
      return "/default/default_banner.png";
    }

    String key = u.getBannerImage();
    String baseUrl = cloudflareProperties.getCdn();

    log.debug("배너 이미지 key={}, baseUrl={}", key, baseUrl);

    if (key == null || key.isEmpty()) {
      String defaultUrl = baseUrl + "/default/default_banner.png";
      log.info("배너 이미지 없음 - userId={}, 기본 이미지 반환: {}", u.getId(), defaultUrl);
      return defaultUrl;
    }

    String imageUrl = baseUrl + "/" + key;
    log.debug("배너 이미지 URL 생성 - userId={}, url={}", u.getId(), imageUrl);
    return imageUrl;
  }
}