package com.chessmate.api.user.dto;

import com.chessmate.common.dto.OAuthPlatForm;

public record UserCardResponse(
    String username,
    OAuthPlatForm platform,
    String profileImageUrl,
    String bannerImageUrl
) {
    public static UserCardResponse from(ProfileResponse profile) {
        return new UserCardResponse(
            profile.username(),
            profile.platform(),
            profile.profileImageUrl(),
            profile.bannerImageUrl()
        );
    }
}
