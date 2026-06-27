package com.netcloud.loadbalancer.algorithms.leastconnections;


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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeastConnectionsAlgorithm implements RoutingAlgorithm {
    private final RoutingService routingService;
    private final ValkeyPoolManager poolManager;
    private static final ConcurrentHashMap<String, Integer> retryCount = new ConcurrentHashMap<>();

    @Override
    public HttpResponse route(HttpServletRequest httpRequest, LoadBalancerDto loadBalancerDto) throws InterruptedException, IOException {
        poolManager.init(loadBalancerDto.getId());
        List<Server> backends = poolManager.selectServers(loadBalancerDto.getId());
        List<Server> shuffled = new ArrayList<>(backends);
        Collections.shuffle(shuffled);
        List<Server> eligible = shuffled.stream()
                .sorted(Comparator.comparingLong(server -> {
                    if (server.getConnectionCount() == null) return 0L;
                    return server.getConnectionCount().get();
                }))
                .toList();
        log.info("The eligible servers are {}", eligible);

        if (eligible.isEmpty()) eligible = backends;

        byte[] body = httpRequest.getInputStream().readAllBytes();
        return routeRequest(0, eligible, loadBalancerDto, httpRequest, body);
    }

    private HttpResponse routeRequest(int index, List<Server> serverList, LoadBalancerDto loadBalancerDto, HttpServletRequest httpRequest, byte[] body) throws IOException {
        Server server = serverList.get(index);
        log.info("Routing to backend server with least connection is {}" , server);
        httpRequest.setAttribute("BACKEND_IP", server.getServerIp());
        poolManager.incrementConnections(server);
        HttpResponse response = routingService.route(httpRequest, server.getServerIp(), loadBalancerDto.getBackendConfig().getPort(), body, loadBalancerDto.getBackendConfig().getRoutingProtocol().getName(), loadBalancerDto.getHealthCheckConfig().getTimeout());
        boolean isSuccess = response != null;
        log.info("Success is {} for ip {}", isSuccess, server.getServerIp());
        poolManager.recordResponse(server, isSuccess, loadBalancerDto.getId());
        if (!isSuccess)
        {
            if (retryCount.getOrDefault(loadBalancerDto.getId(), 0) < serverList.size() - 1)
            {
                retryCount.put(loadBalancerDto.getId(), retryCount.getOrDefault(loadBalancerDto.getId(), 0) + 1);
                return routeRequest(index + 1, serverList, loadBalancerDto, httpRequest, body);
            }
            else
            {
                retryCount.remove(loadBalancerDto.getId());
                throw new BackendUnavailableException("All backend servers failed to respond");
            }
        }
        else
        {
            poolManager.decrementConnections(server);
        }
        retryCount.remove(loadBalancerDto.getId());
        return response;
    }

}
