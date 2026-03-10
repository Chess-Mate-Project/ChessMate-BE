package com.chessmate.api.platform.lichess.controller;


import com.chessmate.api.oauth.OAuth2PrincipalDetails;
import com.chessmate.api.platform.lichess.service.LichessProfileService;
import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/platform/lichess")
public class LichessController {

  private final LichessProfileService lichessProfileService;

  @GetMapping("profile")
  public ResponseEntity<SuccessResponse<ProfileResponse>> getUserProfile(
      @AuthenticationPrincipal OAuth2PrincipalDetails oAuth2PrincipalDetails
  ) {

    lichessProfileService.getProfile(oAuth2PrincipalDetails.getId());

    return ResponseEntity.ok(
        new SuccessResponse<>("사용자 프로필 조회 성공", null)
    );
  }

}
