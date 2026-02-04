package com.chessmate.infra_redis.redis;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisStreamService {

  private final StringRedisTemplate redisTemplate;

  public void publish(String streamKey, Map<String, String> body) {
    redisTemplate.opsForStream()
        .add(MapRecord.create(streamKey, body));
  }

  public void createGroupIfAbsent(String streamKey, String group) {
    try {
      redisTemplate.opsForStream()
          .createGroup(streamKey, ReadOffset.latest(), group);
    } catch (Exception e) {
      // BUSYGROUP이면 무시
    }
  }
}
