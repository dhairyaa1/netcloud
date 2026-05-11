package com.netcloud.cloudrun;

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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudRunService {
    private final RedisTemplate<String, String> redisTemplate;
    private final RoutingService routingService;

    private final ConcurrentHashMap<String, ConsistentHashRing> tenantCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    private static final int VIRTUAL_NODES = 150;

    @PostConstruct
    public void init() {
        log.info("CloudRun Service initialized");
    }

    public HttpResponse<String> route(HttpServletRequest request, String tenantSecret) throws IOException, InterruptedException {

        ConsistentHashRing hashRing = tenantCache.computeIfAbsent(tenantSecret, k -> {
            ConsistentHashRing tb = new ConsistentHashRing();
            tb.setRedis(redisTemplate);
            tb.subscribe();
            return tb;
        });

        long hash = hashLbId(tenantSecret);
        List<Server> servers = hashRing.getServersForHash(hash);
        if (servers.isEmpty()) {
            throw new RuntimeException("No backends available for tenant: " + tenantSecret);
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

    private String roundRobinSelect(List<Server> servers, String tenantSecret) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(tenantSecret, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % servers.size();
        return servers.get(index).getServerIp();
    }

    public static class ConsistentHashRing {
        private final Map<String, Server> activeServers = new ConcurrentHashMap<>();
        private final TreeMap<Long, Server> hashRing = new TreeMap<>();
        private int port = 8080;
        private RedisMessageListenerContainer container;
        private RedisTemplate<String, String> redis;

        public ConsistentHashRing() {
        }

        public void setRedis(RedisTemplate<String, String> redis) {
            this.redis = redis;
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

        public void syncFromValkey() {
            Set<String> members = redis.opsForSet().members("consistent:ring");
            if (members != null) {
                activeServers.clear();
                hashRing.clear();
                for (String ip : members) {
                    Server s = Server.builder()
                            .serverIp(ip)
                            .backendPort(port)
                            .serverState(ServerState.ACTIVE)
                            .build();
                    activeServers.put(ip, s);
                    for (int v = 0; v < VIRTUAL_NODES; v++) {
                        hashRing.put(hashServer(ip, v), s);
                    }
                }
            }
            log.info("Synced {} backends with {} virtual nodes", activeServers.size(), VIRTUAL_NODES);
        }

        public void subscribe() {
            syncFromValkey();
            
            MessageListener listener = (message, pattern) -> {
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                if (payload.startsWith("ADD:")) {
                    String ip = payload.substring(4);
                    Server s = Server.builder()
                            .serverIp(ip)
                            .backendPort(port)
                            .serverState(ServerState.ACTIVE)
                            .build();
                    activeServers.put(ip, s);
                    for (int v = 0; v < VIRTUAL_NODES; v++) {
                        hashRing.put(hashServer(ip, v), s);
                    }
                } else if (payload.startsWith("REMOVE:")) {
                    String ip = payload.substring(7);
                    Server removed = activeServers.remove(ip);
                    if (removed != null) {
                        for (int v = 0; v < VIRTUAL_NODES; v++) {
                            hashRing.remove(hashServer(ip, v));
                        }
                    }
                }
            };

            container = new RedisMessageListenerContainer();
            container.setConnectionFactory(redis.getConnectionFactory());
            container.addMessageListener(listener, new ChannelTopic("consistent:ring"));
            container.afterPropertiesSet();
        }

        public List<Server> getServersForHash(long clientHash) {
            if (hashRing.isEmpty()) {
                return Collections.emptyList();
            }
            java.util.Set<Server> selected = new java.util.HashSet<>();
            for (int i = 0; i < VIRTUAL_NODES; i++) {
                long searchHash = (clientHash + i) & 0x7FFFFFFFFFFFFFFFL;
                Map.Entry<Long, Server> entry = hashRing.ceilingEntry(searchHash);
                if (entry == null) {
                    entry = hashRing.firstEntry();
                }
                if (entry != null) {
                    selected.add(entry.getValue());
                }
            }
            return new ArrayList<>(selected);
        }

        public int getPort() {
            return port;
        }
    }
}