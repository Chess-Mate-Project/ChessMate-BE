package com.chessmate.external.lichess.dto.oauth;


public record OAuthValueRequest(
    String code,
    String codeVerifier
) {

}