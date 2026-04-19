package com.netcloud.loadbalancer_ws.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netcloud.commons.SecretGenerator;
import com.netcloud.commons.dto.Secret;
import com.netcloud.commons.exceptions.ResourceNotFoundException;
import com.netcloud.dtos.loadbalancer.LbConfig;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.loadbalancer_ws.mappers.LBModelDtoMapper;
import com.netcloud.loadbalancer_ws.repositories.LBRepository;
import com.netcloud.models.loadbalancer.LoadBalacerConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoadBalancerConfigService {

    private final LBRepository lbRepository;
    private final LBModelDtoMapper mapper;
    private final SecretGenerator generator;
    private final RedisTemplate<String, LbConfig> valkeyTemplate;

    @Caching(evict = {
            @CacheEvict(value = "loadbalancerByTenant", key = "#loadBalacerDto.tenantId"),
            @CacheEvict(value = "loadbalancers", key = "#loadBalacerDto.tenantId + ':' + #loadBalacerDto.id")
    })
    @Transactional
    public LoadBalancerDto createLoadBalancer(LoadBalancerDto loadBalacerDto) throws JsonProcessingException {
        LoadBalacerConfig loadBalacerConfig = mapper.toConfig(loadBalacerDto);
        loadBalacerConfig =
                lbRepository.save(loadBalacerConfig);
        String secret = generator.generateSecret(loadBalacerDto.getTenantId(), loadBalacerDto.getHealthCheckConfig().getInterval(), loadBalacerConfig.getId());
        loadBalacerConfig.setUrlSecret(secret);
        loadBalacerConfig.setDateCreated(Instant.now());
        loadBalacerDto = mapper.toDto(lbRepository.save(loadBalacerConfig));
        valkeyTemplate.opsForValue().set(String.format("lb:%s:details", loadBalacerConfig.getId()), loadBalacerDto);
        return loadBalacerDto;
    }
    public Secret getSecret(String tenantSecret) {
        return generator.getClaimsFromSecret(tenantSecret);
    }
    @Cacheable(value = "loadbalancers", key =  "#tenantId + ':' + #lbId")
    public LoadBalancerDto getLoadBalancer(String tenantId, String lbId) {
        return mapper.toDto(lbRepository.findByIdAndTenantId(lbId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LoadBalancer not found")));
    }
    @Cacheable(value = "loadbalancerByTenant", key = "#tenantId")
    public List<LoadBalancerDto> getAllByTenant(String tenantId) {
        return mapper.toDtos (lbRepository.findByTenantId(tenantId));
    }
    @Caching(evict = {
            @CacheEvict(value = "loadbalancerByTenant", key = "#tenantId"),
            @CacheEvict(value = "loadbalancers", key = "#tenantId + ':' + #lbId")
    })
    public void deleteLoadBalancer(String tenantId, String lbId) {

        LoadBalacerConfig existing = lbRepository.findByIdAndTenantId(lbId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Load Balancer ID")
                );
        lbRepository.delete(existing);
    }
}
