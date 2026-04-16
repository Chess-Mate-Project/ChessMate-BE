package com.chessmate.api.user.dto;

import com.chessmate.common.dto.OAuthPlatForm;
import lombok.Builder;

@Builder

public record UserSearchProfileResponse (
    Long id,
    int rating,
    String profileImageUrl,
    String username,
    OAuthPlatForm platform
) {}
