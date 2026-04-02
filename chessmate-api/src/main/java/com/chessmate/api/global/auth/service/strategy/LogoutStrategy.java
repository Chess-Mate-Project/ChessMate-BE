package com.chessmate.api.global.auth.service.strategy;

import com.chessmate.common.dto.OAuthPlatForm;
import jakarta.servlet.http.HttpServletResponse;

public interface LogoutStrategy {
  void logout(Long userId, HttpServletResponse response);
  OAuthPlatForm getProvider();
}
