package com.chessmate.infra_redis.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
}
