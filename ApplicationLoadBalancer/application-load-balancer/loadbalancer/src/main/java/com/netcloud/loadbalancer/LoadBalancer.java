package com.netcloud.loadbalancer;

import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@SpringBootApplication
@ComponentScan("com.netcloud")
@Slf4j
public class LoadBalancer {

    public static void main(String[] args) {
        SpringApplication.run(LoadBalancer.class, args);
        log.info("Shared pub/sub listener container started");
    }

}