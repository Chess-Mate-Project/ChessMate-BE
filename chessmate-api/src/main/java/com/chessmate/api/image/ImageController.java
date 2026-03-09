  package com.chessmate.api.image;

  import com.chessmate.api.image.dto.UploadUrlResponse;
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

    @GetMapping("/upload-url")
    public ResponseEntity<SuccessResponse<UploadUrlResponse>> getUploadUrl(
        @AuthenticationPrincipal UserPrincipal u,
        @RequestParam("type") UserImageType type,
        @RequestParam("contentType") String contentType
    ) {

      UploadUrlResponse url = imageService.generateUploadUrl(u.getUser(), type, contentType);

      return ResponseEntity.ok(
          new SuccessResponse<>("이미지 업로드 URL 생성 성공", url)
      );
    }

    @PostMapping("/upload-complete")
    public ResponseEntity<SuccessResponse<Void>> completeUpload(
        @AuthenticationPrincipal UserPrincipal u,
        @RequestParam("type") UserImageType type
    ) {

      imageService.completeUpload(u.getUser(), type);

      return ResponseEntity.ok(
          new SuccessResponse<>("업로드 완료", null)
      );
    }

  }

