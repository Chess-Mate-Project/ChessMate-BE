package com.chessmate.api.user.controller;


import com.chessmate.api.auth.UserPrincipal;
import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.api.user.dto.TotalUserCountResponse;
import com.chessmate.api.user.dto.UpdateUserDescriptionRequest;
import com.chessmate.api.user.service.UserService;
import com.chessmate.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("count")
  public ResponseEntity<SuccessResponse<TotalUserCountResponse>> getTotalUserCount() {
    TotalUserCountResponse response = userService.getTotalUserCount();

    return ResponseEntity.ok(
        new SuccessResponse<>("총 사용자 수 조회 성공", response)
    );
  }

  @GetMapping("profile")
  public ResponseEntity<SuccessResponse<ProfileResponse>> getUserProfile(
      @AuthenticationPrincipal UserPrincipal u
  ) {
    ProfileResponse response = userService.getUserProfile(u.getUser());

    return ResponseEntity.ok(
        new SuccessResponse<>("사용자 프로필 조회 성공", response)
    );
  }

  @PutMapping("/description")
  public ResponseEntity<SuccessResponse<Void>> updateUserDescription(
      @AuthenticationPrincipal UserPrincipal u,
      @RequestBody UpdateUserDescriptionRequest request
  ) {
    userService.updateUserDescription(u.getUser(), request);
    return ResponseEntity.ok(
        new SuccessResponse<>("사용자 소개글 수정 성공", null)
    );
  }



}