package com.netcloud.loadbalancer.factories;

import com.netcloud.commons.exceptions.NotImplementedException;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.enums.RoutingPoliciesEnum;
import com.netcloud.loadbalancer.algorithms.RoutingAlgorithm;
import com.netcloud.loadbalancer.algorithms.leastconnections.LeastConnectionsAlgorithm;
import com.netcloud.loadbalancer.algorithms.roundrobin.RoundRobinAlgorithm;
import com.netcloud.loadbalancer.services.RoutingService;
import com.netcloud.loadbalancer.services.valkey.ValkeyDeadPoolManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RoutingFactory {
    private final RoutingService routingService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ValkeyDeadPoolManager valkeyDeadPoolManager;
    private final ObjectMapper mapper;
    public RoutingAlgorithm routingAlgorithm(LoadBalancerDto dto)
    {
        if(dto.getRoutingAlgorithm() == RoutingPoliciesEnum.ROUND_ROBIN)
        {
            return new RoundRobinAlgorithm(routingService, redisTemplate, mapper);
        }
        else if(dto.getRoutingAlgorithm() == RoutingPoliciesEnum.LEAST_CONNECTION)
        {
            return new LeastConnectionsAlgorithm(routingService, redisTemplate, mapper);
        }
        else
        {
            throw new NotImplementedException();
        }
    }
}
