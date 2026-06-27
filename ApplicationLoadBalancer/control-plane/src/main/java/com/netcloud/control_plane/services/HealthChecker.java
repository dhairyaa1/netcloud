package com.netcloud.control_plane.services;

import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.control_plane.config.ControlPlaneProperties;
import com.netcloud.dtos.servers.Server;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthChecker {

    private final StringRedisTemplate redisTemplate;
    private final ControlPlaneProperties properties;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeMonitors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> backoffLevels = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    @PostConstruct
    public void init() {
        log.info("Control Plane Node [{}] initialized. Partitioning among {} nodes.",
                properties.getInstanceId(), properties.getTotalNodes());
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Control Plane monitors...");
        activeMonitors.values().forEach(f -> f.cancel(true));
        scheduler.shutdownNow();
    }

    public void startMonitoringIfAssigned(String serverKey, Server server) {
        if (activeMonitors.containsKey(serverKey)) {
            log.debug("Already monitoring {}. Ignoring duplicate event.", serverKey);
            return;
        }

        log.info("Assigned server {} to this CP node. Starting health checks with exponential backoff.", serverKey);
        servers.put(serverKey, server);
        backoffLevels.put(serverKey, 0L);
        scheduleNextCheck(serverKey, 0);
    }

    private void scheduleNextCheck(String serverKey, long delaySeconds) {
        Server server = servers.get(serverKey);
        if (server == null) {
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(
                () -> performHealthCheck(serverKey),
                delaySeconds,
                TimeUnit.SECONDS
        );
        activeMonitors.put(serverKey, future);
    }

    private boolean isAssignedToTheNode(String serverKey) {
        int serverHash = Math.abs(hashToInt(serverKey));
        int nodeIndex = Math.abs(hashToInt(properties.getInstanceId()));
        return (serverHash % properties.getTotalNodes()) == (nodeIndex % properties.getTotalNodes());
    }

    private int hashToInt(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) |
                    ((hash[2] & 0xFF) << 8)  | (hash[3] & 0xFF);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void performHealthCheck(String serverKey) {
        Server server = servers.get(serverKey);
        if (server == null) {
            return;
        }
        log.info("performing health check");
        String healthUrl = server.getRoutingProtocol().name() + "://" + server.getServerIp() + ":" + server.getBackendPort() + server.getHealthUrl();
        log.info("Routing to url {}", healthUrl);
        RestTemplate client = new RestTemplate();
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(server.getHealthTimeout() * 1000);
        factory.setReadTimeout(server.getHealthTimeout() * 1000);
        client.setRequestFactory(factory);

        try {
            ResponseEntity<String> response = client.getForEntity(healthUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                handleSuccess(serverKey, server);
            } else {
                log.warn("Server {} health check returned {} - URL: {}", serverKey, response.getStatusCode(), healthUrl);
                handleFailure(serverKey);
            }
        } catch (Exception e) {
            log.error("Server {} health check failed - URL: {}, Error: {}", serverKey, healthUrl, e.getMessage());
            handleFailure(serverKey);
        }
    }

    private void handleSuccess(String serverKey, Server server) {
        String successCounterKey = "cp:success:" + serverKey;
        Long count = redisTemplate.opsForValue().increment(successCounterKey);

        log.info("Incremented {} - new count: {}", successCounterKey, count);
        redisTemplate.expire(successCounterKey, properties.getHealthCheckInterval() * properties.getRecoveryThreshold() * 1000, TimeUnit.MILLISECONDS);

        if (count != null && count >= properties.getRecoveryThreshold()) {
            promoteToActive(serverKey, server);
        } else {
            backoffLevels.put(serverKey, 0L);
            scheduleNextCheck(serverKey, properties.getHealthCheckInterval());
        }
    }

    private void handleFailure(String serverKey) {
        long currentLevel = backoffLevels.getOrDefault(serverKey, 0L);
        long nextInterval = calculateBackoffInterval(currentLevel);

        backoffLevels.put(serverKey, currentLevel + 1);
        log.debug("Server {} health check failed. Next check in {}s (level {})", serverKey, nextInterval, currentLevel + 1);

        scheduleNextCheck(serverKey, nextInterval);
    }

    private long calculateBackoffInterval(long backoffLevel) {
        long interval = (long) (properties.getDeadPoolBaseInterval() * Math.pow(properties.getBackoffMultiplier(), backoffLevel));
        return Math.min(interval, properties.getDeadPoolMaxInterval());
    }

    private void promoteToActive(String serverKey, Server server) {
        String activeKey = String.format("%s:%s", ValkeyPools.ACTIVE_POOL, server.getLbId());
        String deadKey = String.format("%s:%s", ValkeyPools.DEAD_POOL, server.getLbId());

        if (server.getServerIp() == null) {
            log.error("Server IP is null in ServerEvent for key: {}", serverKey);
            stopMonitoring(serverKey);
            return;
        }

        log.info("Promoting server: {} to active pool. Server IP: {}", serverKey, server.getServerIp());

        try {
            redisTemplate.opsForHash().delete(deadKey, server.getServerIp());
            redisTemplate.opsForHash().put(activeKey, server.getServerIp(), mapper.writeValueAsString(server));
            stopMonitoring(serverKey);
            log.info("Server {} recovered. Promoted to active pool & notified LBs.", serverKey);
        } catch (Exception e) {
            log.error("Redis error promoting server: {}", e.getMessage(), e);
        }


    }

    private void stopMonitoring(String serverKey) {
        redisTemplate.opsForValue().getAndDelete("cp:success:" + serverKey);
        ScheduledFuture<?> future = activeMonitors.remove(serverKey);
        if (future != null) future.cancel(true);
        servers.remove(serverKey);
        backoffLevels.remove(serverKey);
    }

}