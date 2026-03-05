package com.chessmate.external.lichess.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OauthAccessTokenDto(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Long expiresIn
) {

}
