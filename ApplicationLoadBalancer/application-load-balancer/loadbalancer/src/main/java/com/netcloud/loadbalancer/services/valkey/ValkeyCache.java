package com.netcloud.loadbalancer.services.valkey;

import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class ValkeyCache {
    private  RedisMessageListenerContainer listenerContainer;
    public static final int FAILURE_THRESHOLD = 2;
    public static final int RECOVERY_THRESHOLD = 2;
    public static final double INITIAL_RECOVERY_WEIGHT = 0.1;
    private final ObjectMapper mapper;
    public static final ConcurrentHashMap<String, HashMap<String,Server>> localActive = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, HashMap<String, Server>> localDead = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> valkeyTemplate; // Identical API to RedisTemplate

    @PostConstruct
    private void startListener() {
        listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(valkeyTemplate.getConnectionFactory());
        listenerContainer.addMessageListener(createActivePoolListener(), new PatternTopic(ValkeyPools.ACTIVE_POOL + ":*"));
        listenerContainer.addMessageListener(createDeadPoolListener(), new PatternTopic(ValkeyPools.DEAD_POOL + ":*"));
        listenerContainer.afterPropertiesSet();
        listenerContainer.start();
        log.info("Shared pub/sub listener container started");
    }
    private MessageListener createDeadPoolListener() {
        return (message, pattern) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String tenantId = extractTenantId(channel);
            log.info("Got the server in dead pool for tenant {}: {}", tenantId, payload);
            Server server = mapper.readValue(payload, Server.class);
            server.setServerState(ServerState.DEAD);
            server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
            server.resetSuccesses();
            changeToDead(server, tenantId);
        };
    }
    private MessageListener createActivePoolListener() {
        return (message, pattern) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String tenantId = extractTenantId(channel);
            log.info("Got the server in active pool for tenant {}: {}", tenantId, payload);
            Server server = mapper.readValue(payload, Server.class);
            server.setServerState(ServerState.RECOVERING);
            server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
            server.resetSuccesses();
            changeToActive(server, tenantId);
        };
    }

    private String extractTenantId(String channel) {
        return channel.substring(channel.lastIndexOf(':') + 1);
    }
    private void changeToActive(Server server, String tenantId)
    {
        HashMap<String, Server> activeMap = localActive.getOrDefault(tenantId, new HashMap<>());
        activeMap.put(server.getServerIp(), server);
        localActive.put(tenantId, activeMap);
        HashMap<String, Server> deadMap = localDead.get(tenantId);
        if (deadMap != null) {
            deadMap.remove(server.getServerIp());
        }
        log.info("Marking the following servers as active {}", localActive);
    }
    public void changeToDead(Server server, String tenantId)
    {
        HashMap<String, Server> deadMap = localDead.getOrDefault(tenantId, new HashMap<>());
        deadMap.put(server.getServerIp(), server);
        localDead.put(tenantId, deadMap);
        HashMap<String, Server> activeMap = localActive.get(tenantId);
        if (activeMap != null) {
            activeMap.remove(server.getServerIp());
        }
        log.info("Marking the following servers as dead {}", localDead);
    }

}