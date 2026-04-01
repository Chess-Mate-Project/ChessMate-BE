package com.chessmate.api.global.auth.oauth.common.dto;

import com.chessmate.common.dto.OAuthPlatForm;

public record OAuthUrlResponse (
    OAuthPlatForm platform,
    String oauthUrl
) {}
