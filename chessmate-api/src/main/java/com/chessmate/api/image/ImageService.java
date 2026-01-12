package com.chessmate.api.image;

import com.chessmate.common.code.ImageErrorCode;
import com.chessmate.common.exception.ImageException;
import com.chessmate.domain.user.User;
import com.chessmate.infra_persistence.repositoryImpl.UserRepositoryImpl;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
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
  public String generateUploadUrl(
      User user,
      UserImageType type
  ) {
    String key = String.format("users/%d/%s.jpg", user.getId(), type.name().toLowerCase());

    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(cloudflareProperties.getBucket())
        .key(key)
        .contentType("image/jpeg")
        .build();

    PutObjectPresignRequest  presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(3))
            .putObjectRequest(putRequest)
            .build();

    return s3Presigner
        .presignPutObject(presignRequest)
        .url()
        .toString();
  }
}