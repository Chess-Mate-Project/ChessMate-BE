package com.chessmate.api.image;

import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

  private final ImageService imageService;

  @PostMapping("/upload-url")
  public ResponseEntity<SuccessResponse<String>> getUploadUrl(
      @AuthenticationPrincipal UserPrincipal u,
      @RequestParam("type") UserImageType type
  ) {
    String url = imageService.generateUploadUrl(u.getUser(), type);

    return ResponseEntity.ok(
        new SuccessResponse<>("이미지 업로드 URL 생성 성공", url)
    );
  }
}