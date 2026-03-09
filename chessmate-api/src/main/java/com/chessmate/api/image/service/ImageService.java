package com.chessmate.api.image.service;

import com.chessmate.api.image.dto.UserImageType;
import com.chessmate.api.image.config.CloudflareProperties;
import com.chessmate.api.image.dto.UploadUrlResponse;
import com.chessmate.infra_core.repository.ProfileRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

  private final ProfileRepository profileRepository;
  private final CloudflareProperties cloudflareProperties;
  private final S3Presigner s3Presigner;

  @Transactional
  public UploadUrlResponse generateUploadUrl(
      Long id,
      UserImageType type,
      String contentType
  ) {
    String url = String.format("users/%d/%s.jpg", id, type.name().toLowerCase());
    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(cloudflareProperties.getBucket())
        .url(url)
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
      Long id,
      UserImageType type
  ) {
    String url = String.format("users/%d/%s.jpg", id, type.name().toLowerCase());

    log.info("[이미지 업로드 완료 시작] userId={}, type={}, url={}", id, type, url);

    if (type == UserImageType.PROFILE) {
      profileRepository.updateProfile(id, url);
      log.info("[프로필 이미지 저장 완료] userId={}, url={}", id, url);
    } else {
      profileRepository.updateBanner(id, url);
      log.info("[배너 이미지 저장 완료] userId={}, url={}", id, url);
    }
  }
}
