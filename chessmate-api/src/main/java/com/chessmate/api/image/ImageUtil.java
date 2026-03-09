package com.chessmate.api.image;

import com.chessmate.api.image.config.CloudflareProperties;
import com.chessmate.infra_core.entity.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 이미지 URL 생성 유틸리티 클래스
 *
 * Cloudflare CDN을 통한 프로필 이미지 및 배너 이미지 URL을 생성합니다.
 * 이미지 키가 없을 경우 기본 이미지 URL을 반환합니다.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ImageUtil {

  private final CloudflareProperties cloudflareProperties;

  /**
   * 프로필 이미지 URL을 생성합니다.
   *
   * @param profile 사용자 프로필 객체 (Profile 엔티티)
   * @return Cloudflare CDN을 통한 프로필 이미지 URL, 없을 경우 기본 이미지 URL
   *
   * @apiNote 프로필 이미지 키가 없거나 비어있으면 기본 이미지(/default/default_profile.png)를 반환합니다.
   */
  public String getProfileImageUrl(Profile profile) {
    log.debug("프로필 이미지 URL 조회 - profileId={}", profile.getId());

    if (cloudflareProperties == null) {
      log.warn("CloudflareProperties가 null입니다. 기본값 반환");
      return "/default/default_profile.png";
    }

    String key = profile.getProfile();
    String baseUrl = cloudflareProperties.getCdn();

    log.debug("프로필 이미지 key={}, baseUrl={}", key, baseUrl);

    if (key == null || key.isEmpty()) {
      String defaultUrl = baseUrl + "/default/default_profile.png";
      log.info("프로필 이미지 없음 - profileId={}, 기본 이미지 반환: {}", profile.getId(), defaultUrl);
      return defaultUrl;
    }

    String imageUrl = baseUrl + "/" + key;
    log.debug("프로필 이미지 URL 생성 - profileId={}, url={}", profile.getId(), imageUrl);
    return imageUrl;
  }

  /**
   * 배너 이미지 URL을 생성합니다.
   *
   * @param profile 사용자 프로필 객체 (Profile 엔티티)
   * @return Cloudflare CDN을 통한 배너 이미지 URL, 없을 경우 기본 이미지 URL
   *
   * @apiNote 배너 이미지 키가 없거나 비어있으면 기본 이미지(/default/default_banner.png)를 반환합니다.
   */
  public String getBannerImageUrl(Profile profile) {
    log.debug("배너 이미지 URL 조회 - profileId={}", profile.getId());

    if (cloudflareProperties == null) {
      log.warn("CloudflareProperties가 null입니다. 기본값 반환");
      return "/default/default_banner.png";
    }

    String key = profile.getBanner();
    String baseUrl = cloudflareProperties.getCdn();

    log.debug("배너 이미지 key={}, baseUrl={}", key, baseUrl);

    if (key == null || key.isEmpty()) {
      String defaultUrl = baseUrl + "/default/default_banner.png";
      log.info("배너 이미지 없음 - profileId={}, 기본 이미지 반환: {}", profile.getId(), defaultUrl);
      return defaultUrl;
    }

    String imageUrl = baseUrl + "/" + key;
    log.debug("배너 이미지 URL 생성 - profileId={}, url={}", profile.getId(), imageUrl);
    return imageUrl;
  }
}