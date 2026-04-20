package com.chessmate.infra_redis.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


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


  /**
   * 큐의 왼쪽(Head)에 데이터를 넣습니다. (Producer)
   *
   * @param key   Redis 큐 키
   * @param value 저장할 데이터
   */
  public void enqueue(String key, Object value) {
    redisTemplate.opsForList().leftPush(key, value);
  }

  /**
   * 큐의 오른쪽(Tail)에서 데이터를 즉시 꺼내옵니다. (Consumer - Non-Blocking)
   * 데이터가 없으면 null을 반환합니다.
   *
   * @param key  Redis 큐 키
   * @param type 꺼낼 데이터 클래스 타입
   * @param <T>  데이터 타입 파라미터
   * @return 꺼낸 데이터 (없으면 null)
   */
  public <T> T dequeue(String key, Class<T> type) {
    Object obj = redisTemplate.opsForList().rightPop(key);
    return convert(obj, type);
  }


  private <T> T convert(Object obj, Class<T> type) {
    if (obj == null) return null;
    return objectMapper.convertValue(obj, type);
  }

}
