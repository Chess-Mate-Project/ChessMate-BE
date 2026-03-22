package com.chessmate.external.dto;

public record OAuthUrlInfoDTO (
    String codeVerifier,
    String state,
    String oauthUrl
) { }
