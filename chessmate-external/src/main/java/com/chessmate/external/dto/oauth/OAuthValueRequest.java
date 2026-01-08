package com.chessmate.external.dto.oauth;


public record OAuthValueRequest(
    String code,
    String codeVerifier
) {

}