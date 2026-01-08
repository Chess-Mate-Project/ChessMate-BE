package com.chessmate.worker.redis;

import com.chessmate.infra_redis.redis.RedisStreamService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisStreamConsumerConfig {

  private static final String STREAM = "user.events";
  private static final String GROUP = "user-group";

  private final RedisConnectionFactory connectionFactory;
  private final RedisStreamService redisStreamService;
  private final UserEventConsumer consumer;

  @Bean
  public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamContainer() {

    // 🔥 이게 없어서 니가 하루 종일 처맞은 거다
    redisStreamService.createGroupIfAbsent(STREAM, GROUP);

    var options =
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions
            .builder()
            .pollTimeout(Duration.ofSeconds(1))
            .build();

    var container =
        StreamMessageListenerContainer.create(connectionFactory, options);

    container.receive(
        Consumer.from(GROUP, "worker-1"),
        StreamOffset.create(STREAM, ReadOffset.lastConsumed()),
        consumer
    );

    container.start();
    return container;
  }
}
