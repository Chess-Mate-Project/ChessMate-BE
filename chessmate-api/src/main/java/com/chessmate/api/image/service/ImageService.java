package com.chessmate.api.image.service;

import com.chessmate.api.image.config.CloudflareProperties;
import com.chessmate.api.image.dto.UploadUrlResponse;
import com.chessmate.api.image.dto.UserImageType;
import com.chessmate.common.code.ImageErrorCode;
import com.chessmate.common.dto.OAuthPlatForm;
import com.chessmate.common.exception.ImageException;
import com.chessmate.domain.chesscom.user.ChesscomUserRepository;
import com.chessmate.domain.lichess.user.LichessUserRepository;
import com.chessmate.infra_redis.redis.RedisService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

  private final LichessUserRepository lichessUserRepository;
  private final ChesscomUserRepository chesscomUserRepository;
  private final CloudflareProperties cloudflareProperties;
  private final S3Presigner s3Presigner;
  private final RedisService redisService;

  /**
   * 이미지 업로드용 Presigned URL 생성
   * - 클라이언트가 이 URL로 직접 R2에 PUT 요청
   * - 유효 시간: 3분
   */
  public UploadUrlResponse generateUploadUrl(
      Long userId,
      OAuthPlatForm platform,
      UserImageType type,
      String contentType
  ) {
    validateContentType(contentType);

    String key = buildR2Key(userId, platform, type);

    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(cloudflareProperties.getBucket())
        .key(key)
        .contentType(contentType)
        .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(3))
        .putObjectRequest(putRequest)
        .build();

    String url;
    try {
      url = s3Presigner.presignPutObject(presignRequest).url().toString();
    } catch (Exception e) {
      log.error("[Presigned URL 생성 실패] userId={}, platform={}, type={}", userId, platform, type, e);
      throw new ImageException(ImageErrorCode.FILE_UPLOAD_TO_CLOUDFLARE_R2_FAILED);
    }

    log.info("[Presigned URL 생성] userId={}, platform={}, type={}", userId, platform, type);
    return new UploadUrlResponse(url);
  }

  /**
   * 업로드 완료 처리
   * - R2 key를 DB에 저장
   * - 프로필 캐시 무효화
   */
  @Transactional
  public void completeUpload(Long userId, OAuthPlatForm platform, UserImageType type) {
    String key = buildR2Key(userId, platform, type);

    log.info("[업로드 완료 처리] userId={}, platform={}, type={}, key={}", userId, platform, type, key);

    if (type == UserImageType.PROFILE) {
      updateProfileImage(userId, platform, key);
    } else {
      updateBannerImage(userId, platform, key);
    }

    redisService.delete(buildProfileCacheKey(userId, platform));
    log.info("[캐시 무효화] userId={}, platform={}", userId, platform);
  }

  /**
   * 현재 저장된 이미지 URL 조회
   * - key가 없으면 기본 이미지 URL 반환
   */
  public UploadUrlResponse getImageUrl(Long userId, OAuthPlatForm platform, UserImageType type) {
    String key = resolveStoredKey(userId, platform, type);

    if (key == null || key.isEmpty()) {
      String defaultUrl = type == UserImageType.PROFILE
          ? cloudflareProperties.getCdn() + "/default/default_profile.png"
          : cloudflareProperties.getCdn() + "/default/default_banner.png";
      log.debug("[기본 이미지 반환] userId={}, platform={}, type={}", userId, platform, type);
      return new UploadUrlResponse(defaultUrl);
    }

    return new UploadUrlResponse(cloudflareProperties.getCdn() + "/" + key);
  }

  // =====================
  // Private helpers
  // =====================

  private void updateProfileImage(Long userId, OAuthPlatForm platform, String key) {
    switch (platform) {
      case LICHESS -> lichessUserRepository.updateProfileImage(userId, key);
      case CHESSCOM -> chesscomUserRepository.updateProfileImage(userId, key);
    }
  }

  private void updateBannerImage(Long userId, OAuthPlatForm platform, String key) {
    switch (platform) {
      case LICHESS -> lichessUserRepository.updateBannerImage(userId, key);
      case CHESSCOM -> chesscomUserRepository.updateBannerImage(userId, key);
    }
  }

  private String resolveStoredKey(Long userId, OAuthPlatForm platform, UserImageType type) {
    return switch (platform) {
      case LICHESS -> lichessUserRepository.findById(userId)
          .map(u -> type == UserImageType.PROFILE ? u.getProfile() : u.getBanner())
          .orElse(null);
      case CHESSCOM -> chesscomUserRepository.findById(userId)
          .map(u -> type == UserImageType.PROFILE ? u.getProfile() : u.getBanner())
          .orElse(null);
    };
  }

  /**
   * R2 저장 경로: users/{platform}/{userId}/{type}.jpg
   */
  private String buildR2Key(Long userId, OAuthPlatForm platform, UserImageType type) {
    return String.format("users/%s/%d/%s.jpg",
        platform.name().toLowerCase(), userId, type.name().toLowerCase());
  }

  private String buildProfileCacheKey(Long userId, OAuthPlatForm platform) {
    return String.format("user:profile:%s:%d", platform.name().toLowerCase(), userId);
  }

  private void validateContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      throw new ImageException(ImageErrorCode.FILENAME_IS_NOT_MISSING);
    }
    if (!contentType.startsWith("image/")) {
      throw new ImageException(ImageErrorCode.CONTENT_TYPE_IS_NOT_IMAGE);
    }
    if (!contentType.equals("image/jpeg")
        && !contentType.equals("image/png")
        && !contentType.equals("image/webp")) {
      throw new ImageException(ImageErrorCode.UNSUPPORTED_FILE_TYPE);
    }
  }
}
