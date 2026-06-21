package com.netcloud.loadbalancer.factories;

import com.netcloud.commons.exceptions.NotImplementedException;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.loadbalancer.algorithms.RoutingAlgorithm;
import com.netcloud.loadbalancer.algorithms.leastconnections.LeastConnectionsAlgorithm;
import com.netcloud.loadbalancer.algorithms.roundrobin.RoundRobinAlgorithm;
import com.netcloud.loadbalancer.services.RoutingService;
import com.netcloud.loadbalancer.services.valkey.ValkeyPoolManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RoutingFactory {
    private final RoutingService routingService;
    private final ValkeyPoolManager valkeyPoolManager;
    public RoutingAlgorithm routingAlgorithm(LoadBalancerDto dto)
    {
        if(dto.getRoutingAlgorithm() == com.netcloud.enums.RoutingAlgorithm.ROUND_ROBIN)
        {
            return new RoundRobinAlgorithm(routingService, valkeyPoolManager);
        }
        else if(dto.getRoutingAlgorithm() == com.netcloud.enums.RoutingAlgorithm.LEAST_CONNECTION)
        {
            return new LeastConnectionsAlgorithm(routingService, valkeyPoolManager);
        }
        else
        {
            throw new NotImplementedException();
        }
    }
}
