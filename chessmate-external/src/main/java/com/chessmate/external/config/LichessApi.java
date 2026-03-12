package com.chessmate.external.config;

import com.chessmate.external.dto.account.LichessAccountDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;

public interface LichessApi {
  @GetExchange(value = "/account", accept = "application/json")
  LichessAccountDto getMyAccount(
      @RequestHeader("Authorization") String bearerToken
  );
}
