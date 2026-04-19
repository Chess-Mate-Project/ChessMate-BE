package com.chessmate.api.user.controller;

import com.chessmate.api.global.auth.dto.UserPrincipal;
import com.chessmate.api.rank.dto.PlatformUserCountResponse;
import com.chessmate.api.user.dto.ProfileResponse;
import com.chessmate.api.user.dto.UpdateUserDescriptionRequest;
import com.chessmate.api.user.dto.UserCardResponse;
import com.chessmate.api.user.service.UserService;
import com.chessmate.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<SuccessResponse<ProfileResponse>> getProfile(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProfileResponse response = userService.getProfile(principal.getId(), principal.getProvider());
        return ResponseEntity.ok(new SuccessResponse<>("프로필 조회 성공", response));
    }

    /**
     * 카드 섹션용 간략 정보 조회 (프로필/배너/이름/플랫폼)
     * GET /api/user/card
     */
    @GetMapping("/card")
    public ResponseEntity<SuccessResponse<UserCardResponse>> getCard(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProfileResponse profile = userService.getProfile(principal.getId(), principal.getProvider());
        return ResponseEntity.ok(new SuccessResponse<>("카드 조회 성공", UserCardResponse.from(profile)));
    }

    /**
     * 자기소개 수정
     * PUT /api/user/description
     */
    @PutMapping("/description")
    public ResponseEntity<SuccessResponse<Void>> updateDescription(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody UpdateUserDescriptionRequest request
    ) {
        userService.updateDescription(principal.getId(), principal.getProvider(), request.description());
        return ResponseEntity.ok(new SuccessResponse<>("자기소개 수정 성공", null));
    }

    @GetMapping("/platform-stats")
    public ResponseEntity<SuccessResponse<PlatformUserCountResponse>> getPlatformStats() {
        PlatformUserCountResponse response = userService.getPlatformUserCounts();
        return ResponseEntity.ok(new SuccessResponse<>("플랫폼별 유저 수 조회 성공", response));
    }
}
