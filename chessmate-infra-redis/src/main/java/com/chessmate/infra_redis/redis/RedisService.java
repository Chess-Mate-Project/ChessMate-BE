package com.chessmate.infra_redis.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;


    /**
     * DTO 객체를 그대로 Redis에 저장합니다.
     *
     * @param key               Redis 키 (예: "user:alice")
     * @param value             저장할 DTO 객체
     * @param expirationSeconds TTL(초 단위)
     */
    public void save(String key, Object value, long expirationSeconds) {
        redisTemplate.opsForValue()
                .set(key, value, expirationSeconds, TimeUnit.SECONDS);
    }


    /**
     * Redis에서 꺼내올 때는 호출자가 원하는 타입으로 캐스팅합니다.
     *
     * @param key  Redis 키
     * @param type 꺼낼 DTO 클래스 타입
     * @param <T>  DTO 타입 파라미터
     * @return 저장된 DTO 인스턴스 (없으면 null)
     */
    public <T> T get(String key, Class<T> type) {
        Object obj = redisTemplate.opsForValue().get(key);

        if (obj == null) return null;
        return objectMapper.convertValue(obj, type);
    }


    /**
     * 키 삭제
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 키 존재 여부 확인
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }



    /// ///


  /**
   * 큐의 왼쪽(Head)에 데이터를 넣습니다. (Producer)
   */
  public void leftPush(String key, Object value) {
    redisTemplate.opsForList().leftPush(key, value);
  }

  /**
   * 큐의 오른쪽(Tail)에서 데이터를 꺼내옵니다. (Consumer - Blocking)
   * 데이터가 없으면 timeout 동안 대기합니다.
   */
  public <T> T brPop(String key, long timeoutSeconds, Class<T> type) {
    // bRPop은 리스트 형식으로 [Key, Value]를 반환하므로 index 1을 가져옵니다.
    Object obj = redisTemplate.execute((RedisCallback<Object>) connection -> {
      java.util.List<byte[]> result = connection.bRPop((int) timeoutSeconds, key.getBytes());
      if (result == null || result.isEmpty()) return null;
      return redisTemplate.getValueSerializer().deserialize(result.get(1));
    });

    if (obj == null) return null;
    return objectMapper.convertValue(obj, type);
  }

  /**
   * 여러 큐를 동시에 감시하다가 데이터가 들어오는 쪽에서 꺼내옵니다. (우선순위 큐용)
   */
  public <T> T brPopMultiple(long timeoutSeconds, Class<T> type, String... keys) {
    Object obj = redisTemplate.execute((RedisCallback<Object>) connection -> {
      byte[][] byteKeys = java.util.Arrays.stream(keys)
          .map(String::getBytes)
          .toArray(byte[][]::new);

      java.util.List<byte[]> result = connection.bRPop((int) timeoutSeconds, byteKeys);
      if (result == null || result.isEmpty()) return null;
      return redisTemplate.getValueSerializer().deserialize(result.get(1));
    });

    if (obj == null) return null;
    return objectMapper.convertValue(obj, type);
  }
}
