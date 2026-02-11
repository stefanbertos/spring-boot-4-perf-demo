package com.example.perftester.redis;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    @Scheduled(fixedRate = 300000)
    public void pushDummyValues() {
        var timestamp = Instant.now().toEpochMilli();
        var uuid = UUID.randomUUID().toString();

        var key = "perf:dummy:" + timestamp;
        var value = uuid + "-" + timestamp;
        redisTemplate.opsForValue().set(key, value);

        redisTemplate.opsForValue().increment("perf:counter");

        log.debug("Pushed dummy Redis value: {} = {}", key, value);
    }
}
