package com.netcloud.loadbalancer.algorithms.leastconnections;


import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import com.netcloud.dtos.servers.ServerState;
import com.netcloud.loadbalancer.algorithms.RoutingAlgorithm;
import com.netcloud.loadbalancer.services.RoutingService;
import com.netcloud.loadbalancer.services.valkey.ValkeyDeadPoolManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class LeastConnectionsAlgorithm implements RoutingAlgorithm {
    private final RoutingService routingService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper;
    @Override
    public HttpResponse route(HttpServletRequest httpRequest, LoadBalancerDto loadBalancerDto) throws InterruptedException, IOException {
        ValkeyDeadPoolManager poolManager = new ValkeyDeadPoolManager(redisTemplate, mapper);
        poolManager.init(loadBalancerDto.getTenantId());
        List<Server> backends = poolManager.selectServers(loadBalancerDto.getTenantId());
        List<Server> eligible = backends.stream()
                .filter(s -> s.getServerState() == ServerState.ACTIVE ||
                        ThreadLocalRandom.current().nextDouble() < s.getTrafficWeight())
                .toList();

        if (eligible.isEmpty()) eligible = backends;

        Server backend = eligible.stream()
                .min(Comparator.comparingLong(server -> server.getConnectionCount().get())).orElse(backends.get(0));
        httpRequest.setAttribute("BACKEND_IP", backend.getIpAddress());
        poolManager.incrementConnections(backend.getIpAddress(), loadBalancerDto.getTenantId());
        HttpResponse response = routingService.route(httpRequest, backend.getIpAddress(), loadBalancerDto.getBackendConfig().getPort(), loadBalancerDto.getTenantId(), loadBalancerDto.getId());
        poolManager.recordResponse(backend, response.statusCode() >= 200 && response.statusCode() <300, loadBalancerDto.getTenantId(), loadBalancerDto.getId());
        return response;
    }
}
