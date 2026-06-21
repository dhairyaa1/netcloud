package com.netcloud.loadbalancer.business;

import com.netcloud.commons.dto.Secret;
import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.ServerState;
import com.netcloud.loadbalancer.algorithms.RoutingAlgorithm;
import com.netcloud.loadbalancer.factories.RoutingFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor
public class RoutingBAL {
    private final RoutingFactory factory;
    private final RedisTemplate<String, String> valkeyTemplate;
    private final ObjectMapper mapper;
    public HttpResponse route(HttpServletRequest request, Secret secret) throws IOException, InterruptedException {

        LoadBalancerDto loadBalancerDto = mapper.readValue(valkeyTemplate.opsForValue().get(String.format("lb:%s:details", secret.getLbId())), LoadBalancerDto.class);
        RoutingAlgorithm algorithm = factory.routingAlgorithm(loadBalancerDto);
        return algorithm.route(request, loadBalancerDto);
    }
}
