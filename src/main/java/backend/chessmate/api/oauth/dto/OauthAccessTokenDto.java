package backend.chessmate.api.oauth.dto;

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
