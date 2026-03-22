package com.chessmate.api.oauth.common.dto;

import com.chessmate.external.type.OAuthPlatForm;

public record OAuthUrlResponse (
    OAuthPlatForm platform,
    String oauthUrl
) {}
