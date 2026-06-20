package com.netcloud.cloudrun;

import com.netcloud.commons.SecretGenerator;
import com.netcloud.commons.dto.Secret;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudRunService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RoutingService routingService;
    private final CloudRunProperties properties;
    private final ConcurrentHashMap<String, ConsistentHashRing> tenantCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Set<String> globalServers = ConcurrentHashMap.newKeySet();

    private static final int MAX_TOTAL_VNODES = 150;
    private static final int MIN_VNODES_PER_SERVER = 10;
    private static final String GLOBAL_RING_KEY = "lb:active";

    @PostConstruct
    public void init() {
        syncFromValkey();
        subscribe();
        log.info("CloudRun Service initialized with {} active servers", globalServers.size());
    }

    private void syncFromValkey() {
        Set<String> servers = redisTemplate.opsForSet().members(GLOBAL_RING_KEY);
        if (servers != null && !servers.isEmpty()) {
            globalServers.addAll(servers);
            log.info("Synced {} active servers from {}", globalServers.size(), GLOBAL_RING_KEY);
        } else {
            log.warn("No active servers found in {}", GLOBAL_RING_KEY);
        }
    }

    private void subscribe() {
        MessageListener listener = (message, pattern) -> {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            if (payload.startsWith("ADD:")) {
                String ip = payload.substring(4);
                globalServers.add(ip);
                log.debug("Server added to global pool: {}", ip);
            } else if (payload.startsWith("REMOVE:")) {
                String ip = payload.substring(7);
                globalServers.remove(ip);
                for (ConsistentHashRing ring : tenantCache.values()) {
                    ring.handleServerRemoved(ip);
                }
                log.debug("Server removed from global pool: {}", ip);
            }
        };

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        container.addMessageListener(listener, new ChannelTopic(GLOBAL_RING_KEY));
        container.afterPropertiesSet();
        container.start();
        log.info("Subscribed to {} channel", GLOBAL_RING_KEY);
    }

    public HttpResponse<String> route(HttpServletRequest request, String tenantSecret) throws IOException, InterruptedException {
        ConsistentHashRing hashRing = tenantCache.computeIfAbsent(tenantSecret, k -> {
            ConsistentHashRing ring = new ConsistentHashRing(tenantSecret);
            ring.buildRingFromGlobalPool(globalServers);
            return ring;
        });

        long hash = hashLbId(tenantSecret);
        List<Server> servers = hashRing.getServersForHash(hash);
        if (servers.isEmpty()) {
            throw new RuntimeException("No backends available for secret: " + tenantSecret);
        }
        else {
            log.info("servers selected are {}", servers);
        }

        String serverIp = roundRobinSelect(servers, tenantSecret);
        request.setAttribute("BACKEND_IP", serverIp);

        return routingService.route(request, serverIp, hashRing.port);
    }

    private long hashLbId(String lbId) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = sha.digest(lbId.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash lbId", e);
        }
    }

    private String roundRobinSelect(List<Server> servers, String lbId) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(lbId, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % servers.size();
        return servers.get(index).getServerIp();
    }

    public class ConsistentHashRing {
        private final Map<String, Server> activeServers = new ConcurrentHashMap<>();
        private final TreeMap<Long, Server> hashRing = new TreeMap<>();
        private final String lbId;
        private int vnodeCount;
        private final int port = 80;

        public ConsistentHashRing(String lbId) {
            this.lbId = lbId;
        }

        public void buildRingFromGlobalPool(Set<String> pool) {
            activeServers.clear();
            hashRing.clear();
            List<String> sorted = new ArrayList<>(pool);
            if (sorted.isEmpty()) {
                log.warn("No servers in global pool");
                return;
            }
            int count = Math.min(properties.getServersPerTenant(), sorted.size());
            List<String> assigned = sorted.subList(0, count);
            buildRingFromIps(new HashSet<>(assigned));
            log.info("Assigned {} backends to lbId {}", count, lbId);
        }

        private long hashServer(String ip, int vnode) {
            try {
                String key = ip + "-vnode-" + vnode;
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = sha.digest(key.getBytes(StandardCharsets.UTF_8));
                long hash = 0;
                for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
                    hash = (hash << 8) | (hashBytes[i] & 0xFF);
                }
                return hash;
            } catch (Exception e) {
                throw new RuntimeException("Failed to hash server", e);
            }
        }

        private void buildRingFromIps(Set<String> ips) {
            activeServers.clear();
            hashRing.clear();
            int serverCount = Math.max(ips.size(), 1);
            vnodeCount = Math.max(MIN_VNODES_PER_SERVER, MAX_TOTAL_VNODES / serverCount);
            for (String ip : ips) {
                Server s = Server.builder()
                        .serverIp(ip)
                        .backendPort(port)
                        .serverState(ServerState.ACTIVE)
                        .build();
                activeServers.put(ip, s);
                for (int v = 0; v < vnodeCount; v++) {
                    hashRing.put(hashServer(ip, v), s);
                }
            }
        }

        public void handleServerRemoved(String ip) {
            if (activeServers.remove(ip) != null) {
                if (activeServers.size() < properties.getServersPerTenant()) {
                    replaceServer();
                }
                rebuildHashRing();
            }
        }

        private void replaceServer() {
            List<String> available = new ArrayList<>(globalServers);
            available.removeAll(activeServers.keySet());
            if (available.isEmpty()) return;
            String replacement = available.get(0);
            Server s = Server.builder()
                    .serverIp(replacement)
                    .backendPort(port)
                    .serverState(ServerState.ACTIVE)
                    .connectionCount(new AtomicLong(0))
                    .build();
            activeServers.put(replacement, s);
            log.info("Replaced removed server for lbId {} with {}", lbId, replacement);
        }

        private void rebuildHashRing() {
            hashRing.clear();
            int serverCount = Math.max(activeServers.size(), 1);
            vnodeCount = Math.max(MIN_VNODES_PER_SERVER, MAX_TOTAL_VNODES / serverCount);
            for (Map.Entry<String, Server> entry : activeServers.entrySet()) {
                for (int v = 0; v < vnodeCount; v++) {
                    hashRing.put(hashServer(entry.getKey(), v), entry.getValue());
                }
            }
            log.debug("Rebuilt hash ring for lbId {}: {} servers, {} vnodes each", lbId, activeServers.size(), vnodeCount);
        }

        public List<Server> getServersForHash(long clientHash) {
            if (hashRing.isEmpty()) {
                return Collections.emptyList();
            }
            int replicas = properties.getReplicas();
            long searchHash = clientHash & 0x7FFFFFFFFFFFFFFFL;
            Map.Entry<Long, Server> start = hashRing.ceilingEntry(searchHash);
            if (start == null) {
                start = hashRing.firstEntry();
            }
            if (start == null) {
                return Collections.emptyList();
            }
            Set<Server> selected = new LinkedHashSet<>();
            NavigableMap<Long, Server> tail = hashRing.tailMap(start.getKey(), true);
            for (Map.Entry<Long, Server> e : tail.entrySet()) {
                selected.add(e.getValue());
                if (selected.size() >= replicas) break;
            }
            if (selected.size() < replicas) {
                for (Map.Entry<Long, Server> e : hashRing.entrySet()) {
                    selected.add(e.getValue());
                    if (selected.size() >= replicas) break;
                }
            }
            return new ArrayList<>(selected);
        }

        public int getPort() {
            return port;
        }
    }
}
