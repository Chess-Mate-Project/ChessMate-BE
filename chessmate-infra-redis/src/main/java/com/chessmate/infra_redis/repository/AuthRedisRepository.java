package com.chessmate.infra_redis.repository;


import com.chessmate.infra_redis.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuthRedisRepository {

  private final RedisService redisService;


}

