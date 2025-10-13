package backend.chessmate.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuthValueRequest {
    @Schema(description = "OAuth 인증 코드", example = "abc123")
    private String code;

    @Schema(description = "PKCE code verifier", example = "xyz456")
    private String codeVerifier;
}