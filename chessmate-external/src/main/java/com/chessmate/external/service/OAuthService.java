package com.chessmate.external.service;

import com.chessmate.external.config.LichessProperties;
import com.chessmate.external.config.ChesscomProperties;
import com.chessmate.external.dto.OAuthUrlInfoDTO;
import com.chessmate.external.oauth.chesscom.ChesscomOauthApi;
import com.chessmate.external.oauth.lichess.LichessOauthApi;
import com.chessmate.external.type.OAuthPlatForm;
import com.chessmate.external.util.PkceUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * OAuth 인증 URL 생성 및 플랫폼별 설정 관리 서비스
 */
@Service
@RequiredArgsConstructor
public class OAuthService {
  private final LichessProperties lichessProperties;
  private final ChesscomProperties chesscomProperties;
  private final LichessOauthApi lichessOauthApi;
  private final ChesscomOauthApi chesscomOauthApi;

  /**
   * 플랫폼에 따라 OAuth 인증 URL을 생성합니다.
   *
   * @param platForm OAuth 플랫폼 (LICHESS 또는 CHESSCOM)
   * @return 생성된 OAuth 인증 URL
   */
  public OAuthUrlInfoDTO generateOauthUrl(OAuthPlatForm platForm) {
    String codeVerifier = PkceUtil.generateRandomCodeVerifier();
    String codeChallengeMethod = "S256";
    String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);
    String responseType = "code";
    String state = PkceUtil.generateRandomState();

    String clientId;
    String oauthUrl;
    String redirectUri;
    Map<String, String> params = new LinkedHashMap<>();

    if (platForm == OAuthPlatForm.LICHESS) {
      clientId = lichessProperties.getClientId();
      oauthUrl = lichessProperties.getOauthUrl() + "/oauth";
      redirectUri = lichessProperties.getRedirectUri();


      params.put("response_type", responseType);
      params.put("client_id", clientId);
      params.put("code_challenge", codeChallenge);
      params.put("code_challenge_method", codeChallengeMethod);
      params.put("state", state);
      params.put("redirect_uri", redirectUri);
    } else if (platForm == OAuthPlatForm.CHESSCOM) {
      clientId = chesscomProperties.getClientId();
      oauthUrl = chesscomProperties.getOauthUrl() + "/authorize";
      redirectUri = chesscomProperties.getRedirectUri();

      params.put("response_type", responseType);
      params.put("client_id", clientId);
      params.put("code_challenge", codeChallenge);
      params.put("code_challenge_method", codeChallengeMethod);
      params.put("state", state);
      params.put("redirect_uri", redirectUri);
      params.put("scope", "openid profile email");
    } else {
      throw new IllegalArgumentException("지원하지 않는 플랫폼입니다: " + platForm);
    }

    String parameterString = buildQueryString(params);

    return new OAuthUrlInfoDTO(codeVerifier, state, oauthUrl + "?" + parameterString);
  }

  public Map<String, Object> getTokenApiResponse(OAuthPlatForm platForm, String code, String codeVerifier) {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("code", code);
    body.add("grant_type", "authorization_code");
    body.add("code_verifier", codeVerifier);

    if (platForm == OAuthPlatForm.CHESSCOM) {

      body.add("redirect_uri", chesscomProperties.getRedirectUri());
      body.add("client_id", chesscomProperties.getClientId());

      return chesscomOauthApi.getToken(body);
    } else if (platForm == OAuthPlatForm.LICHESS) {
      body.add("redirect_uri", lichessProperties.getRedirectUri());
      body.add("client_id", lichessProperties.getClientId());

      return lichessOauthApi.getToken(body);
    } else {
      throw new IllegalArgumentException("지원하지 않는 플랫폼입니다: " + platForm);
    }


  }
  /**
   * Map의 파라미터들을 URL 인코딩된 쿼리 스트링으로 변환합니다.
   *
   * @param params 파라미터 맵
   * @return URL 인코딩된 쿼리 스트링
   */
  private String buildQueryString(Map<String, String> params) {
    return params.entrySet().stream()
        .map(entry -> {
          try {
            return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
          } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL 인코딩 실패", e);
          }
        })
        .reduce((a, b) -> a + "&" + b)
        .orElse("");

  }


}
