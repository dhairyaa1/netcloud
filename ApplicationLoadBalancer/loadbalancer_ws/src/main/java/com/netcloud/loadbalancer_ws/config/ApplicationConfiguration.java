package com.netcloud.loadbalancer_ws.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcloud.dtos.loadbalancer.LbConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;

@Configuration
@Getter
@Slf4j
public class ApplicationConfiguration {
    @Value("${netcloud.loadbalancer.url}")
    private String lbUrl;

    @Value("${netcloud.autodiscovery.url}")
    private String autodiscoveryUrl;

    @Value(("${netcloud.assignable-server-count}"))
    private int assignableServers;

    @PostConstruct
    public void logConfig() {
        log.info("[CONFIG] AUTODISCOVERY_URL env: {}", System.getenv("AUTODISCOVERY_URL"));
        log.info("[CONFIG] Resolved autodiscovery.url: {}", autodiscoveryUrl);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RedisTemplate<String, LbConfig> valkeyTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {

        RedisTemplate<String, LbConfig> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Keys: UTF-8 strings
        template.setKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));

        // Values: JSON-serialized LbConfig objects
        Jackson2JsonRedisSerializer<LbConfig> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, LbConfig.class);
        template.setValueSerializer(valueSerializer);

        // Also set hash serializers if you use Hash operations
        template.setHashKeySerializer(new StringRedisSerializer(StandardCharsets.UTF_8));
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

}
