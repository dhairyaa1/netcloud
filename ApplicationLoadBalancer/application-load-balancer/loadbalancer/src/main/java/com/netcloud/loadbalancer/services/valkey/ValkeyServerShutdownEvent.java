package com.netcloud.loadbalancer.services.valkey;

import jakarta.annotation.PreDestroy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ValkeyServerShutdownEvent {
    private RedisTemplate redisTemplate;

}
