//package com.netcloud.loadbalancer.configuration;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//
//import java.time.Duration;
//
//@Configuration
//public class CacheManagerConfig {
//    @Bean
//    public RedisConnectionFactory redisTenantSpecificConnectionFactory() {
//        RedisStandaloneConfiguration config =
//                new RedisStandaloneConfiguration("redis1.host", 6379);
//
//        //config.setPassword("password1");
//
//        return new LettuceConnectionFactory(config);
//    }
//
//    @Bean
//    public RedisConnectionFactory redisRoutingConnectionFactory() {
//        RedisStandaloneConfiguration config =
//                new RedisStandaloneConfiguration("redis2.host", 6379);
//
//        //config.setPassword("password2");
//
//        return new LettuceConnectionFactory(config);
//    }
//    @Bean
//    public RedisCacheManager redisTenantSpecificConnectionManager(
//            @Qualifier("redisTenantSpecificConnectionFactory") RedisConnectionFactory factory) {
//
//        RedisCacheConfiguration config =
//                RedisCacheConfiguration.defaultCacheConfig()
//                        .entryTtl(Duration.ofMinutes(5));
//
//        return RedisCacheManager.builder(factory)
//                .cacheDefaults(config)
//                .build();
//    }
//
//    @Bean
//    public RedisCacheManager redisRoutingConnectionManager(
//            @Qualifier("redisRoutingConnectionFactory") RedisConnectionFactory factory) {
//
//        RedisCacheConfiguration config =
//                RedisCacheConfiguration.defaultCacheConfig()
//                        .entryTtl(Duration.ofMinutes(30));
//
//        return RedisCacheManager.builder(factory)
//                .cacheDefaults(config)
//                .build();
//    }
//    @Bean
//    public RedisTemplate<String, Object> redisTenantSpecificTemplate(
//            @Qualifier("redisTenantSpecificConnectionFactory") RedisConnectionFactory factory) {
//
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//        return template;
//    }
//    @Bean
//    public RedisTemplate<String, Object> redisRoutingTemplate(
//            @Qualifier("redisRoutingConnectionFactory") RedisConnectionFactory factory) {
//
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//        return template;
//    }
//}
