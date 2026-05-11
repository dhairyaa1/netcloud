package com.netcloud.control_plane.services;

import com.netcloud.commons.enums.CacheEventTypes;
import com.netcloud.commons.enums.ValkeyPools;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Service
@Slf4j
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;
    public void markAsActive(String lbId, String ipaddress)
    {
        try {
            String key = String.format("lb:%s:details", lbId);
            LoadBalancerDto loadBalancerDto = mapper.readValue(redisTemplate.opsForValue().get(key), LoadBalancerDto.class);
            Server server = Server.builder()
                    .serverIp(ipaddress)
                    .healthTimeout(loadBalancerDto.getHealthCheckConfig().getTimeout())
                    .backendPort(loadBalancerDto.getBackendConfig().getPort())
                    .healthUrl(loadBalancerDto.getHealthCheckConfig().getEndpoint())
                    .healthCheckInterval(loadBalancerDto.getHealthCheckConfig().getInterval())
                    .tenantId(loadBalancerDto.getTenantId())
                    .eventType(CacheEventTypes.MOVED_TO_ACTIVEPOOL.toString())
                    .lbId(lbId)
                    .build();
            redisTemplate.convertAndSend(ValkeyPools.ACTIVE_POOL,
                    mapper.writeValueAsString(server));
            redisTemplate.opsForHash().put(String.format("%s:%s", ValkeyPools.ACTIVE_POOL, server.getTenantId()), server.getServerIp(), mapper.writeValueAsString(server));
        }
        catch (Exception e)
        {
            log.info("An exception encountered while marking tenant backend as active. {}", e.getMessage());
        }
    }

}
