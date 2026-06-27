package com.netcloud.loadbalancer.algorithms.roundrobin;

import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import com.netcloud.loadbalancer.algorithms.RoutingAlgorithm;
import com.netcloud.loadbalancer.exceptions.BackendUnavailableException;
import com.netcloud.loadbalancer.services.RoutingService;
import com.netcloud.loadbalancer.services.valkey.ValkeyPoolManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    private final ValkeyPoolManager poolManager;
    private static final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> retryCount = new ConcurrentHashMap<>();

    @Override
    public HttpResponse route(HttpServletRequest httpRequest, LoadBalancerDto loadBalancerDto) throws InterruptedException, IOException {
        poolManager.init(loadBalancerDto.getId());
        List<Server> backends = poolManager.selectServers(loadBalancerDto.getId());

        if (backends == null || backends.isEmpty()) {
            throw new BackendUnavailableException("No active servers available");
        }
        List<Server> serverList = List.copyOf(backends);
        byte[] body = httpRequest.getInputStream().readAllBytes();
        return routeRequest(getNextIndex(loadBalancerDto.getTenantId(), serverList.size()), serverList, loadBalancerDto, httpRequest, body);
    }
    private HttpResponse routeRequest(int index, List<Server> serverList, LoadBalancerDto loadBalancerDto, HttpServletRequest httpRequest, byte[] body) throws IOException {
        Server server = serverList.get(index);
        String selectedIp = server.getServerIp();
        log.info("The index is {}", index);
        HttpResponse response = routingService.route(httpRequest, selectedIp, loadBalancerDto.getBackendConfig().getPort(), body, loadBalancerDto.getBackendConfig().getRoutingProtocol().getName(), loadBalancerDto.getHealthCheckConfig().getTimeout());
        boolean isSuccess = response != null;
        log.info("Success is {} for ip {}", isSuccess, server.getServerIp());
        poolManager.recordResponse(server, isSuccess, loadBalancerDto.getId());
        if(!isSuccess)
        {
            if(retryCount.getOrDefault(loadBalancerDto.getId(), 0) <= serverList.size()) {
                retryCount.put(loadBalancerDto.getId(), retryCount.getOrDefault(loadBalancerDto.getId(), 0) + 1);
                return routeRequest(getNextIndex(loadBalancerDto.getTenantId(), serverList.size()), serverList, loadBalancerDto, httpRequest, body);
            }
            else
            {
                retryCount.remove(loadBalancerDto.getId());
                throw new BackendUnavailableException("All backend servers failed to respond");
            }
        }
        return response;
    }
    private int getNextIndex(String tenantId, int serverCount) {
        if (serverCount == 0) return 0;
        AtomicInteger counter = counters.computeIfAbsent(tenantId, k -> new AtomicInteger(0));
        return counter.getAndIncrement() % serverCount;
    }

}