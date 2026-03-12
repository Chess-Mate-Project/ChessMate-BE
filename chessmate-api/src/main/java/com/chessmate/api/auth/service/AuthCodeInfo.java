package com.chessmate.api.auth.service;

import com.chessmate.api.auth.OAuth2Provider;

public record AuthCodeInfo (
    Long userId,
    OAuth2Provider provider
) {}
