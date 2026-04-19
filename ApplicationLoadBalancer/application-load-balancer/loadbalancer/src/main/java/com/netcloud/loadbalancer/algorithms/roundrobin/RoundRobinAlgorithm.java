package com.netcloud.loadbalancer.algorithms.roundrobin;

import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import com.netcloud.loadbalancer.algorithms.RoutingAlgorithm;
import com.netcloud.loadbalancer.services.RoutingService;
import com.netcloud.loadbalancer.services.valkey.ValkeyDeadPoolManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RoundRobinAlgorithm implements RoutingAlgorithm {
    private final RoutingService routingService;
    private final ValkeyDeadPoolManager poolManager;
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public HttpResponse route(HttpServletRequest httpRequest, LoadBalancerDto loadBalancerDto) throws InterruptedException, IOException {

        poolManager.init(loadBalancerDto.getTenantId());
        List<Server> backends = poolManager.selectServers(loadBalancerDto.getTenantId());

        if (backends == null || backends.isEmpty()) {
            throw new RuntimeException("No active servers available");
        }

        List<Server> serverList = List.copyOf(backends);
        int index = getNextIndex(loadBalancerDto.getTenantId(), serverList.size());
        Server server = serverList.get(index);
        String selectedIp = server.getServerIp();
        httpRequest.setAttribute("BACKEND_IP", selectedIp);
        HttpResponse response = routingService.route(httpRequest, selectedIp, loadBalancerDto.getBackendConfig().getPort(), loadBalancerDto.getTenantId(), loadBalancerDto.getId());
        poolManager.recordResponse(server, response.statusCode() >= 200 && response.statusCode() < 300, loadBalancerDto.getTenantId(), loadBalancerDto.getId());
        return response;
    }

    private int getNextIndex(String tenantId, int serverCount) {
        if (serverCount == 0) return 0;
        AtomicInteger counter = counters.computeIfAbsent(tenantId, k -> new AtomicInteger(0));
        return counter.getAndIncrement() % serverCount;
    }
}