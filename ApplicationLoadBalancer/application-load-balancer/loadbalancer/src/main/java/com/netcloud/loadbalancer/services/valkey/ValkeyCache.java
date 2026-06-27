package com.netcloud.loadbalancer.services.valkey;

import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class ValkeyCache {
    private  RedisMessageListenerContainer listenerContainer;
    public static final int FAILURE_THRESHOLD = 2;
    public static final int RECOVERY_THRESHOLD = 2;
    public static final double INITIAL_RECOVERY_WEIGHT = 0.1;
    private final ObjectMapper mapper;
    public static final ConcurrentHashMap<String, HashMap<String,Server>> localActive = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, HashMap<String, Server>> localDead = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> valkeyTemplate;

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
            try {
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
                String lbId = channel.substring(channel.lastIndexOf(':') + 1);
                log.info("Got the server in dead pool for lbId {}: {}", lbId, payload);
                Server server = mapper.readValue(payload, Server.class);
                server.setServerState(ServerState.DEAD);
                server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
                server.resetSuccesses();
                changeToDead(server, lbId);
            } catch (Exception e) {
                log.error("Failed to process dead pool message", e);
            }
        };
    }
    private MessageListener createActivePoolListener() {
        return (message, pattern) -> {
            try {
                log.info("Got some active servers");
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
                String lbId = channel.substring(channel.lastIndexOf(':') + 1);
                log.info("Got the server in active pool for lbId {}: {}", lbId, payload);
                Server server = mapper.readValue(payload, Server.class);
                server.setServerState(ServerState.RECOVERING);
                server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
                server.resetSuccesses();
                changeToActive(server, lbId);
            } catch (Exception e) {
                log.error("Failed to process active pool message", e);
            }
        };
    }

    private void changeToActive(Server server, String lbId)
    {
        localActive.computeIfAbsent(lbId, k -> new HashMap<>())
                .put(server.getServerIp(), server);
        log.info("localactive now is {}", localActive);
        HashMap<String, Server> deadMap = localDead.get(lbId);
        if (deadMap != null) {
            deadMap.remove(server.getServerIp());
        }
        log.info("Marking the following servers as active {}", localActive);
    }
    public void changeToDead(Server server, String lbId)
    {
        localDead.computeIfAbsent(lbId, k -> new HashMap<>())
                .put(server.getServerIp(), server);
        HashMap<String, Server> activeMap = localActive.get(lbId);
        if (activeMap != null) {
            activeMap.remove(server.getServerIp());
        }
        log.info("Marking the following servers as dead {}", localDead);
    }

}
