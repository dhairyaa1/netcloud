package com.netcloud.loadbalancer.algorithms.roundrobin;

import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import com.netcloud.loadbalancer.algorithms.RoutingAlgorithm;
import com.netcloud.loadbalancer.services.RoutingService;
import com.netcloud.loadbalancer.services.valkey.ValkeyDeadPoolManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoundRobinAlgorithm implements RoutingAlgorithm {
    private final RoutingService routingService;
    private final RedisTemplate<String, String> valkeyTemplate;
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, ValkeyDeadPoolManager> valkeyDeadpoolmanagerMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    @Override
    public HttpResponse route(HttpServletRequest httpRequest, LoadBalancerDto loadBalancerDto) throws InterruptedException, IOException {
        final int retryCount = 0;
        ValkeyDeadPoolManager poolManager = valkeyDeadpoolmanagerMap.computeIfAbsent(loadBalancerDto.getTenantId(), k -> {
            ValkeyDeadPoolManager deadPoolManager = new ValkeyDeadPoolManager(valkeyTemplate, mapper);
            deadPoolManager.init(k);
            return deadPoolManager;
        });

        List<Server> backends = poolManager.selectServers(loadBalancerDto.getTenantId());

        if (backends == null || backends.isEmpty()) {
            throw new RuntimeException("No active servers available");
        }
        List<Server> serverList = List.copyOf(backends);
        byte[] body = httpRequest.getInputStream().readAllBytes();
        return routeRequest(getNextIndex(loadBalancerDto.getTenantId(), serverList.size()), serverList, loadBalancerDto, httpRequest, body, retryCount);
    }

    private HttpResponse routeRequest(int index, List<Server> serverList, LoadBalancerDto loadBalancerDto, HttpServletRequest httpRequest, byte[] body, int retryCount) throws IOException {
        Server server = serverList.get(index);
        String selectedIp = server.getServerIp();
        log.info("The index is {}", index);
        HttpResponse response = routingService.route(httpRequest, selectedIp, loadBalancerDto.getBackendConfig().getPort(), body);
        boolean isSuccess = response != null;
        log.info("Success is {} for ip {}", isSuccess, server.getServerIp());
        ValkeyDeadPoolManager manager = valkeyDeadpoolmanagerMap.get(loadBalancerDto.getTenantId());
        if (manager != null) {
            manager.recordResponse(server, isSuccess, loadBalancerDto.getTenantId(), loadBalancerDto.getId());
        }
        if(!isSuccess && retryCount <= serverList.size())
        {
            retryCount++;
            return routeRequest(getNextIndex(loadBalancerDto.getTenantId(), serverList.size()), serverList, loadBalancerDto, httpRequest, body, retryCount);
        }
        return response;
    }
    private int getNextIndex(String tenantId, int serverCount) {
        if (serverCount == 0) return 0;
        AtomicInteger counter = counters.computeIfAbsent(tenantId, k -> new AtomicInteger(0));
        return counter.getAndIncrement() % serverCount;
    }
}