package com.chessmate.api.global.auth.dto;

import lombok.Setter;


public record TokenResponse (
    String accessToken,
    long accessTokenExpiresIn,
    String refreshToken,
    long refreshTokenExpiresIn,
    String tokenType
){}
