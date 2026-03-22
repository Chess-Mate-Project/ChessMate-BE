package com.chessmate.external.oauth.chesscom;


import java.util.Map;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface ChesscomOauthApi {

  @PostExchange("/token")
  Map<String, Object> getToken(@RequestBody MultiValueMap<String, String> body);

}