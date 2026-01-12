package com.chessmate.api.image;

import com.chessmate.api.image.dto.UploadUrlResponse;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.entity.UserEntity;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

  private final S3Client s3Client;
  private final UserRepositoryImpl userRepository;
  private final CloudflareProperties cloudflareProperties;
  private final S3Presigner s3Presigner;

  @Transactional
  public UploadUrlResponse generateUploadUrl(
      User user,
      UserImageType type,
      String contentType
  ) {
    String key = String.format("users/%d/%s.jpg", user.getId(), type.name().toLowerCase());
    String extension = contentType.split("/")[1];
    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(cloudflareProperties.getBucket())
        .key(key)
        .contentType(contentType)
        .build();

    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(3))
            .putObjectRequest(putRequest)
            .build();

    var url = s3Presigner
        .presignPutObject(presignRequest)
        .url()
        .toString();

    return new UploadUrlResponse(url);
  }

  @Transactional
  public void completeUpload(
      User user,
      UserImageType type
  ) {
    String key = String.format("users/%d/%s.jpg", user.getId(), type.name().toLowerCase());
    if (type == UserImageType.PROFILE) {
      userRepository.updateProfileImage(user.getId(), key);
      log.info("프로필 이미지 업로드 완료: userId={}, key={}", user.getId(), key);
    } else {
      userRepository.updateBannerImage(user.getId(), key);
      log.info("배너 이미지 업로드 완료: user)Id={}, key={}", user.getId(), key);
    }
  }

  public UploadUrlResponse getImageUrl(
      User user,
      UserImageType type
  ) {
    User u = userRepository.findById(user.getId())
        .orElseThrow();

    String key = type == UserImageType.PROFILE
        ? u.getProfileImage()
        : u.getBannerImage();

    log.info("key? = " + key);
    if (key == null) {

      log.info("key == null 이라서 기본 이미지 제공 ");
      switch (type) {
        case PROFILE -> {
          return new UploadUrlResponse(
              cloudflareProperties.getCdn() + "/default/default_profile.png");
        }
        case BANNER -> {
          return new UploadUrlResponse(
              cloudflareProperties.getCdn() + "/default/default_banner.png");
        }
      }
    }

    return new UploadUrlResponse(cloudflareProperties.getCdn() + "/" + key);
  }
}
