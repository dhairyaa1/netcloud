package com.netcloud.loadbalancer.services.valkey;


import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

import static com.netcloud.loadbalancer.services.valkey.ValkeyCache.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class ValkeyDeadPoolManager {

    private final RedisTemplate<String, String> valkeyTemplate; // Identical API to RedisTemplate
    private final ObjectMapper mapper;

    public void init(String tenantId) {
        log.info("Inside init");
        syncLocalCacheFromValkey(tenantId);
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
            log.info("{} failures are there", failures);
            valkeyTemplate.opsForHash().put(String.format("%s:%s", ValkeyPools.ACTIVE_POOL, server.getTenantId()), server.getServerIp(), mapper.writeValueAsString(server));
            if (failures >= FAILURE_THRESHOLD) {
                moveToDeadPool(server, tenantId);
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

    private void moveToDeadPool(Server server, String tenantId) {
        String activeKey = String.format("%s:%s", ValkeyPools.ACTIVE_POOL, tenantId);
        String deadKey = String.format("%s:%s", ValkeyPools.DEAD_POOL, tenantId);
        server.setServerState(ServerState.DEAD);
        server.setTrafficWeight(0.0);
        valkeyTemplate.opsForHash().delete(activeKey, server.getServerIp());
        valkeyTemplate.opsForHash().put(deadKey, server.getServerIp(), mapper.writeValueAsString(server));
        log.info("Added to deadpool");
        valkeyTemplate.convertAndSend(getDeadEventChannel(tenantId),
                mapper.writeValueAsString(server));

        HashMap<String, Server> activeMap = localActive.get(tenantId);
        if (activeMap != null) {
            activeMap.remove(server.getServerIp());
        }
        HashMap<String, Server> deadMap = localDead.getOrDefault(tenantId, new HashMap<>());
        deadMap.put(server.getServerIp(), server);
        localDead.put(tenantId, deadMap);

        log.warn("Server {} moved to dead pool after {} failures", server.getServerIp(), FAILURE_THRESHOLD);
    }

    private void handleRecoveryProgress(Server server, String tenantId) {
        if (server.incSuccesses() >= RECOVERY_THRESHOLD) {
            restoreToActive(server, tenantId);
        }
    }

    private void restoreToActive(Server server, String tenantId) {
        String activeKey = String.format("%s:%s", ValkeyPools.ACTIVE_POOL, tenantId);
        String deadKey = String.format("%s:%s", ValkeyPools.DEAD_POOL, tenantId);

        valkeyTemplate.opsForHash().delete(activeKey, server.getServerIp());

       server.setServerState(ServerState.RECOVERING);
        server.setTrafficWeight(INITIAL_RECOVERY_WEIGHT);
        server.resetSuccesses();
        valkeyTemplate.opsForHash().delete(deadKey, server.getServerIp());
        valkeyTemplate.opsForHash().put(activeKey, server.getServerIp(), server);
        valkeyTemplate.convertAndSend(getActiveEventChannel(server.getTenantId()),
                mapper.writeValueAsString(server));

//        HashMap<String, Server> activeMap = localActive.getOrDefault(tenantId, new HashMap<String, Server>());
//        activeMap.put(server.getIpAddress(), server);
//        localActive.put(tenantId, activeMap);
//
//        HashMap<String, Server> deadMap = localDead.getOrDefault(tenantId, new HashMap<String, Server>());
//        deadMap.remove(server.getIpAddress());
//        localDead.put(tenantId, deadMap);
        log.info("Server {} restored to active pool with {:.0f}% traffic ramp-up", server.getServerIp(), INITIAL_RECOVERY_WEIGHT * 100);
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
            log.info("Marking the following servers as active {}", activeServers);
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
            log.info("FMarking the following servers as dead {}", deadServers);
        }
    }




    private String getActiveEventChannel(String tenantId) { return String.format("%s:%s", ValkeyPools.ACTIVE_POOL, tenantId); }
    private String getDeadEventChannel(String tenantId) { return String.format("%s:%s", ValkeyPools.DEAD_POOL, tenantId); }
}
