package com.netcloud.loadbalancer.services.valkey;

import com.netcloud.commons.utils.GCPUtil;
import com.netcloud.commons.utils.HostUtil;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsistentRingPublisher {

    private static final String CHANNEL = "lb:active";
    private final RedisTemplate<String, String> valkeyTemplate;
    private String ipAddress;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            log.info("Inside applicationready.");
            ipAddress = GCPUtil.getInternalIp();
            valkeyTemplate.opsForSet().add(CHANNEL, ipAddress);
            valkeyTemplate.convertAndSend(CHANNEL, "ADD:" + ipAddress);
            log.info("Published startup event to '{}' channel: {}", CHANNEL, ipAddress);
        } catch (Exception e) {
            log.error("Failed to publish startup event to '{}' channel", CHANNEL, e);
        }
    }

    @PreDestroy
    public void onExit() {
        valkeyTemplate.opsForSet().remove(CHANNEL, ipAddress);
        valkeyTemplate.convertAndSend(CHANNEL, "REMOVE:" + ipAddress);
    }
}
