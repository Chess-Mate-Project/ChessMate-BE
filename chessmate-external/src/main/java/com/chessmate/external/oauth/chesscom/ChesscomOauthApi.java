package com.chessmate.external.oauth.chesscom;


import com.chessmate.external.dto.chesscom.ChesscomJwtCertResponse;
import com.chessmate.external.dto.chesscom.ChesscomTokenResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ChesscomOauthApi {

  /**
   * Authorization Code를 이용해 Access Token 발급
   *
   * @param body grant_type, code, code_verifier, redirect_uri, client_id 포함
   * @return ChesscomTokenResponse
   */
  @PostExchange("/token")
  ChesscomTokenResponse getToken(@RequestBody MultiValueMap<String, String> body);

  /**
   * Refresh Token을 이용해 새로운 Access Token 발급
   *
   * @param body grant_type, refresh_token, client_id 포함
   * @return ChesscomTokenResponse
   */
  @PostExchange("/token")
  ChesscomTokenResponse refreshToken(@RequestBody MultiValueMap<String, String> body);

  @GetExchange("/certs")
  ChesscomJwtCertResponse getCert();
}