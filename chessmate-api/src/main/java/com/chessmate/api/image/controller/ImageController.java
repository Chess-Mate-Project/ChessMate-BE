package com.chessmate.api.image.controller;

import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.image.dto.UploadUrlResponse;
import com.chessmate.api.image.dto.UserImageType;
import com.chessmate.api.image.service.ImageService;
import com.chessmate.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

  private final ImageService imageService;

  /**
   * Presigned URL 발급
   * 클라이언트는 이 URL로 직접 R2에 PUT 요청하여 이미지를 업로드합니다.
   *
   * GET /api/image/upload-url?type=PROFILE&contentType=image/jpeg
   */
  @GetMapping("/upload-url")
  public ResponseEntity<SuccessResponse<UploadUrlResponse>> getUploadUrl(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam("type") UserImageType type,
      @RequestParam("contentType") String contentType
  ) {
    UploadUrlResponse response = imageService.generateUploadUrl(
        principal.getId(), principal.getProvider(), type, contentType);

    return ResponseEntity.ok(new SuccessResponse<>("이미지 업로드 URL 생성 성공", response));
  }

  /**
   * 업로드 완료 처리
   * R2에 업로드 후 이 API를 호출하면 DB에 이미지 key가 저장됩니다.
   *
   * POST /api/image/upload-complete?type=PROFILE
   */
  @PostMapping("/upload-complete")
  public ResponseEntity<SuccessResponse<Void>> completeUpload(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam("type") UserImageType type
  ) {
    imageService.completeUpload(principal.getId(), principal.getProvider(), type);

    return ResponseEntity.ok(new SuccessResponse<>("업로드 완료", null));
  }

  /**
   * 현재 이미지 URL 조회
   * key가 없으면 기본 이미지 URL을 반환합니다.
   *
   * GET /api/image/url?type=PROFILE
   */
  @GetMapping("/url")
  public ResponseEntity<SuccessResponse<UploadUrlResponse>> getImageUrl(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam("type") UserImageType type
  ) {
    UploadUrlResponse response = imageService.getImageUrl(
        principal.getId(), principal.getProvider(), type);

    return ResponseEntity.ok(new SuccessResponse<>("이미지 URL 조회 성공", response));
  }
}
