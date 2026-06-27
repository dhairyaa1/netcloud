package com.netcloud.control_plane.valkey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcloud.commons.enums.ValkeyPools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
public class ValkeyConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ValkeyDeadpoolListener valkeyDeadpoolListener) {

        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(valkeyDeadpoolListener, new PatternTopic(ValkeyPools.DEAD_POOL + ":*"));
        log.info("Redis listener container started successfully");
        return container;
    }
}