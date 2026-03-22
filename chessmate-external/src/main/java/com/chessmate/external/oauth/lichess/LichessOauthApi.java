package com.chessmate.external.oauth.lichess;

import java.util.Map;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Lichess OAuth Token 엔드포인트를 호출하기 위한 HTTP 서비스 인터페이스
 * REST Client를 통해 프록시 구현됩니다.
 */
@HttpExchange
public interface LichessOauthApi {

  /**
   * Lichess OAuth 토큰 요청
   *
   * @param body 토큰 요청 본문 (grant_type, code, client_id, redirect_uri 등)
   * @return 토큰 응답 맵 (access_token, token_type, expires_in 등)
   */
  @PostExchange("/api/token")
  Map<String, Object> getToken(@RequestBody MultiValueMap<String, String> body);
}

