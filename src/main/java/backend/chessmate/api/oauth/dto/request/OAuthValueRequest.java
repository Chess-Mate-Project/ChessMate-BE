package backend.chessmate.api.oauth.dto.request;


public record OAuthValueRequest(
    String code,
    String codeVerifier
) {

}