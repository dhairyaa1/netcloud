package com.netcloud.loadbalancer.services.valkey;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.netcloud.commons.enums.CacheEventTypes;
import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import com.netcloud.loadbalancer.exceptions.BackendUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.netcloud.loadbalancer.services.valkey.ValkeyCache.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class ValkeyPoolManager {

    private final RedisTemplate<String, String> valkeyTemplate;
    private final ObjectMapper mapper;
    private final Set<String> initializedLbIds = ConcurrentHashMap.newKeySet();

    public void init(String lbId) {
        if (initializedLbIds.add(lbId)) {
            log.info("Initializing pool manager for lbId {}", lbId);
            syncLocalCacheFromValkey(lbId);
        }
    }

    public void recordResponse(Server server, boolean success, String lbId) {
        if (server == null) return;

        if (success) {
            server.resetFailures();
            if (server.getServerState() == ServerState.RECOVERING) {
                handleRecoveryProgress(server, lbId);
            }
        } else {
            int failures = server.incFailures();
            log.info("{} failures are there", failures);
            try {
                valkeyTemplate.opsForHash().put(String.format("%s:%s", ValkeyPools.ACTIVE_POOL, lbId), server.getServerIp(), mapper.writeValueAsString(server));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize server for failure record", e);
            }
            if (failures >= FAILURE_THRESHOLD) {
                moveToDeadPool(server, lbId);
            }
        }

    }

    public List<Server> selectServers(String lbId) {
        HashMap<String, Server> activeMap = localActive.getOrDefault(lbId, new HashMap<>());
        List<Server> candidates = activeMap.values().stream()
                .filter(s -> s.getServerState() != ServerState.DEAD)
                .toList();

        if (candidates.isEmpty()) throw new BackendUnavailableException("No backend servers available for routing");

        return candidates;
    }

    public void incrementConnections(Server server) {
        server.incConnections();
    }

    public void decrementConnections(Server server) {
        server.decConnections();
    }

    private void moveToDeadPool(Server server, String lbId) {
        try {
            String activeKey = String.format("%s:%s", ValkeyPools.ACTIVE_POOL, lbId);
            String deadKey = String.format("%s:%s", ValkeyPools.DEAD_POOL, lbId);
            server.setServerState(ServerState.DEAD);
            server.setTrafficWeight(0.0);
            server.setEventType(CacheEventTypes.MOVED_TO_DEADPOOL.toString());
            valkeyTemplate.opsForHash().delete(activeKey, server.getServerIp());
            valkeyTemplate.opsForHash().put(deadKey, server.getServerIp(), mapper.writeValueAsString(server));
            log.info("Added to deadpool");
            valkeyTemplate.convertAndSend(getDeadEventChannel(lbId),
                    mapper.writeValueAsString(server));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize server for dead pool", e);
            return;
        }

        HashMap<String, Server> activeMap = localActive.get(lbId);
        if (activeMap != null) {
            activeMap.remove(server.getServerIp());
        }
        localDead.computeIfAbsent(lbId, k -> new HashMap<>())
                .put(server.getServerIp(), server);

        log.warn("Server {} moved to dead pool after {} failures", server.getServerIp(), FAILURE_THRESHOLD);
    }

    private void handleRecoveryProgress(Server server, String lbId) {
        if (server.incSuccesses() >= RECOVERY_THRESHOLD) {
            restoreToActive(server, lbId);
        }
    }

    private void restoreToActive(Server server, String lbId) {
        try {
            String activeKey = String.format("%s:%s", ValkeyPools.ACTIVE_POOL, lbId);
            String deadKey = String.format("%s:%s", ValkeyPools.DEAD_POOL, lbId);

            valkeyTemplate.opsForHash().delete(activeKey, server.getServerIp());

            server.setServerState(ServerState.RECOVERING);
            server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
            server.resetSuccesses();
            server.setEventType(CacheEventTypes.MOVED_TO_ACTIVEPOOL.toString());
            valkeyTemplate.opsForHash().delete(deadKey, server.getServerIp());
            valkeyTemplate.opsForHash().put(activeKey, server.getServerIp(), mapper.writeValueAsString(server));
            valkeyTemplate.convertAndSend(getActiveEventChannel(lbId),
                    mapper.writeValueAsString(server));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize server for active pool restore", e);
            return;
        }

        localActive.computeIfAbsent(lbId, k -> new HashMap<>())
                .put(server.getServerIp(), server);
        HashMap<String, Server> deadMap = localDead.get(lbId);
        if (deadMap != null) {
            deadMap.remove(server.getServerIp());
        }
        log.info("Server {} restored to active pool with {:.0f}% traffic ramp-up", server.getServerIp(), INITIAL_RECOVERY_WEIGHT * 100);
    }

     private void syncLocalCacheFromValkey(String lbId) {
        try {
            Map<Object, Object> activeServerStr = valkeyTemplate.opsForHash().entries(String.format("%s:%s", ValkeyPools.ACTIVE_POOL, lbId));
            HashMap<String, Server> activeServers = activeServerStr.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> {
                                try { return mapper.readValue((String) e.getValue(), Server.class); }
                                catch (JsonProcessingException ex) { throw new RuntimeException(ex); }
                            },
                            (v1, v2) -> v1,
                            HashMap::new
                    ));
            if (!activeServers.isEmpty()) {
                localActive.put(lbId, activeServers);
                log.info("Marking the following servers as active {}", activeServers);
            }
            Map<Object, Object> deadServersStr = valkeyTemplate.opsForHash().entries(String.format("%s:%s", ValkeyPools.DEAD_POOL, lbId));
            HashMap<String, Server> deadServers = deadServersStr.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> (String) e.getKey(),
                            e -> {
                                try { return mapper.readValue((String) e.getValue(), Server.class); }
                                catch (JsonProcessingException ex) { throw new RuntimeException(ex); }
                            },
                            (v1, v2) -> v1,
                            HashMap::new
                    ));
            if (!deadServers.isEmpty()) {
                localDead.put(lbId, deadServers);
                log.info("FMarking the following servers as dead {}", deadServers);
            }
        } catch (RuntimeException e) {
            log.error("Failed to sync local cache from Valkey", e.getCause());
        }
    }

    private String getActiveEventChannel(String lbId) { return String.format("%s:%s", ValkeyPools.ACTIVE_POOL, lbId); }
    private String getDeadEventChannel(String lbId) { return String.format("%s:%s", ValkeyPools.DEAD_POOL, lbId); }
}
