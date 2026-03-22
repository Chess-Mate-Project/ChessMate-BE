package com.chessmate.api.global.auth.service;

import com.chessmate.api.global.auth.OAuth2Provider;

public record AuthCodeInfo (
    Long userId,
    OAuth2Provider provider
) {}
