package com.netcloud.loadbalancer.services.valkey;


import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ValkeyDeadPoolManager {

    private final RedisTemplate<String, String> valkeyTemplate; // Identical API to RedisTemplate
    private final ObjectMapper mapper;
    private static final ConcurrentHashMap<String, HashMap<String,Server>> localActive = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, HashMap<String, Server>> localDead = new ConcurrentHashMap<>();
    private static final int FAILURE_THRESHOLD = 2;
    private static final int RECOVERY_THRESHOLD = 2;
    private static final double INITIAL_RECOVERY_WEIGHT = 0.1;

    public void init(String tenantId) {
        syncLocalCacheFromValkey(tenantId);
        subscribeToActivePoolEvents(tenantId);
        subscribeToDeadPoolEvents(tenantId);
    }

    /**
     * Passive health tracking: Called after each backend response
     */
    public void recordResponse(Server server, boolean success, String tenantId, String lbId) {
        if (server == null) return;

        if (success) {
            server.resetFailures();
            if (server.getServerState() == ServerState.RECOVERING) {
                handleRecoveryProgress(server, tenantId);
            }
        } else {
            int failures = server.incFailures();
            if (failures >= FAILURE_THRESHOLD) {
                moveToDeadPool(server.getIpAddress(), server, tenantId, lbId);
            }
        }
    }

    /**
     * Least Connections selection with recovery ramp-up
     */
    public List<Server> selectServers(String tenantId) {
        HashMap<String, Server> activeMap = localActive.getOrDefault(tenantId, new HashMap<>());
        List<Server> candidates = activeMap.values().stream()
                .filter(s -> s.getServerState() != ServerState.DEAD)
                .toList();

        if (candidates.isEmpty()) throw new RuntimeException("Servers not available");

        return candidates;
    }

    public void incrementConnections(String ip, String tenantId) {
        Server s = localActive.get(tenantId).get(ip);
        if (s != null) s.incConnections();
    }

    public void markRequestEnd(String ip, String tenantId) {
        Server s = localActive.get(tenantId).get(ip);
        if (s != null) s.decConnections();
    }

    private void moveToDeadPool(String ip, Server server, String tenantId, String lbId) {
        String activeKey = ValkeyConstants.getKey(ServerState.ACTIVE, lbId);
        String deadKey = ValkeyConstants.getKey(ServerState.DEAD, lbId);
        String channel = getDeadEventChannel(tenantId);

        valkeyTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) conn -> {
            conn.sRem(activeKey.getBytes(), ip.getBytes());
            conn.sAdd(deadKey.getBytes(), ip.getBytes());
            conn.publish(channel.getBytes(),
                    (deadKey).getBytes(StandardCharsets.UTF_8));
            return null;
        });
        log.info("Moved to dead pool");
        server.setServerState(ServerState.DEAD);
        server.setTrafficWeight(0.0);
        HashMap<String, Server> activeMap = localActive.getOrDefault(tenantId, new HashMap<String, Server>());
        activeMap.remove(ip);
        localActive.put(tenantId, activeMap);

        HashMap<String, Server> deadMap = localDead.getOrDefault(tenantId, new HashMap());
        deadMap.put(ip, server);
        localDead.put(tenantId, deadMap);
        log.warn("Server {} moved to dead pool after {} failures", ip, FAILURE_THRESHOLD);
    }

    private void handleRecoveryProgress(Server server, String tenantId) {
        if (server.incSuccesses() >= RECOVERY_THRESHOLD) {
            restoreToActive(server, tenantId);
        }
    }

    private void restoreToActive(Server server, String tenantId) {
        String activeKey = String.format("%s:%s", ValkeyPools.ACTIVE_POOL, tenantId);
        String deadKey = String.format("%s:%s", ValkeyPools.DEAD_POOL, tenantId);

        valkeyTemplate.opsForHash().delete(activeKey, server.getIpAddress());

       server.setServerState(ServerState.RECOVERING);
        server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
        server.resetSuccesses();
        valkeyTemplate.opsForHash().delete(deadKey, server.getIpAddress());
        valkeyTemplate.opsForHash().put(activeKey, server.getIpAddress(), server);
        valkeyTemplate.convertAndSend(ValkeyPools.ACTIVE_POOL,
                mapper.writeValueAsString(server));

//        HashMap<String, Server> activeMap = localActive.getOrDefault(tenantId, new HashMap<String, Server>());
//        activeMap.put(server.getIpAddress(), server);
//        localActive.put(tenantId, activeMap);
//
//        HashMap<String, Server> deadMap = localDead.getOrDefault(tenantId, new HashMap<String, Server>());
//        deadMap.remove(server.getIpAddress());
//        localDead.put(tenantId, deadMap);
        log.info("Server {} restored to active pool with {:.0f}% traffic ramp-up", server.getIpAddress(), INITIAL_RECOVERY_WEIGHT * 100);
    }

     private void syncLocalCacheFromValkey(String tenantId) {
        Map<Object, Object> activeServerStr = valkeyTemplate.opsForHash().entries(String.format("%s:%s", ValkeyPools.ACTIVE_POOL, tenantId));
         HashMap<String, Server> activeServers = activeServerStr.entrySet().stream()
                 .collect(Collectors.toMap(
                         e -> (String) e.getKey(),
                         e -> mapper.readValue((String) e.getValue(), Server.class),
                         (v1, v2) -> v1, // merge function (shouldn't happen with distinct keys)
                         HashMap::new    // force HashMap output
                 ));
        if (!activeServers.isEmpty()) {
            localActive.put(tenantId, activeServers);
        }
         Map<Object, Object> deadServersStr = valkeyTemplate.opsForHash().entries(String.format("%s:%s", ValkeyPools.DEAD_POOL, tenantId));
         HashMap<String, Server> deadServers = deadServersStr.entrySet().stream()
                 .collect(Collectors.toMap(
                         e -> (String) e.getKey(),
                         e -> mapper.readValue((String) e.getValue(), Server.class),
                         (v1, v2) -> v1, // merge function (shouldn't happen with distinct keys)
                         HashMap::new    // force HashMap output
                 ));
        if (!deadServers.isEmpty()) {
            localDead.put(tenantId, deadServers);
        }
    }

    private void subscribeToActivePoolEvents(String tenantId) {
        MessageListener listener = (message, pattern) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Got the server in active pool {}", payload);
            Server server = mapper.readValue(payload, Server.class);
            HashMap<String, Server> deadMap = localDead.get(tenantId);
            deadMap.remove(server.getIpAddress());
            server.setServerState(ServerState.RECOVERING);
            server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
            server.resetSuccesses();
            changeToActive(server, tenantId);

        };

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(valkeyTemplate.getConnectionFactory());
        container.addMessageListener(listener, new ChannelTopic(getActiveEventChannel(tenantId)));
        container.afterPropertiesSet();
    }
    private void subscribeToDeadPoolEvents(String tenantId) {
        MessageListener listener = (message, pattern) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("Got the server in dead pool {}", payload);
            Server server = mapper.readValue(payload, Server.class);
            HashMap<String, Server> activeMap = localActive.get(tenantId);
            activeMap.remove(server.getIpAddress());
            server.setServerState(ServerState.DEAD);
            server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
            server.resetSuccesses();
            changeToDead(server, tenantId);

        };

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(valkeyTemplate.getConnectionFactory());
        container.addMessageListener(listener, new ChannelTopic(getDeadEventChannel(tenantId)));
        container.afterPropertiesSet();
    }
    private void changeToActive(Server server, String tenantId)
    {
        HashMap<String, Server> activeMap = localActive.getOrDefault(tenantId, new HashMap<>());
        activeMap.put(server.getIpAddress(), server);
        localActive.put(tenantId, activeMap);
    }
    private void changeToDead(Server server, String tenantId)
    {
        HashMap<String, Server> deadMap = localDead.getOrDefault(tenantId, new HashMap<>());
        deadMap.put(server.getIpAddress(), server);
        localActive.put(tenantId, deadMap);
    }
    private String getActiveEventChannel(String tenantId) { return String.format("%s:%s", ValkeyPools.ACTIVE_POOL, tenantId); }
    private String getDeadEventChannel(String tenantId) { return String.format("%s:%s", ValkeyPools.DEAD_POOL, tenantId); }
}
