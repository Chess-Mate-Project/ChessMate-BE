package com.chessmate.external.oauth.lichess;

import com.chessmate.external.dto.lichess.LichessTokenResponse;
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
   * Authorization Code를 이용해 Access Token 발급
   *
   * @param body grant_type, code, code_verifier, redirect_uri, client_id 포함
   * @return LichessTokenResponse
   */
  @PostExchange("/api/token")
  LichessTokenResponse getToken(@RequestBody MultiValueMap<String, String> body);
}

