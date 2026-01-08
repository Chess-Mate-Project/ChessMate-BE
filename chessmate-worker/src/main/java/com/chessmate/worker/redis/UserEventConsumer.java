package com.chessmate.worker.redis;

import com.chessmate.common.dto.UserEvent;
import com.chessmate.worker.batch.service.UserBatchServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer
    implements StreamListener<String, MapRecord<String, String, String>> {

  private final ObjectMapper objectMapper;
  private final UserBatchServiceImpl userBatchService;
  @Override
  public void onMessage(MapRecord<String, String, String> message) {

    try {
      UserEvent event =
          objectMapper.convertValue(message.getValue(), UserEvent.class);

      log.info("🔥 Redis Event 수신: {}", event.type());

      if ("USER_CREATED".equals(event.type())) {
        // 여기서 batch 트리거 or 서비스 호출
        log.info("유저 처리 시작 userId={}", event.
      userId());
        userBatchService.triggerUserUpdate(event.userId(), event.lichessToken());
      }

    } catch (Exception e) {
      log.error("이벤트 처리 실패", e);
    }
  }
}
