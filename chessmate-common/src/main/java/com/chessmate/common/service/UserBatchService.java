package com.chessmate.common.service;

public interface UserBatchService {
  void triggerUserUpdate(Long userId, String lichessToken);
}