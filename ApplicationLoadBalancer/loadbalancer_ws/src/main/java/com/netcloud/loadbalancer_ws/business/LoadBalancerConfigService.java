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
        loadBalacerConfig.setDateCreated(Instant.now());
        String secret = generator.generateSecret(loadBalacerDto.getTenantId(), loadBalacerDto.getHealthCheckConfig().getInterval(), loadBalacerConfig.getId());
        loadBalacerConfig.setUrlSecret(secret);
        loadBalacerConfig = lbRepository.save(loadBalacerConfig);
        loadBalacerDto = mapper.toDto(loadBalacerConfig);
        valkeyTemplate.opsForValue().set(String.format("lb:%s:details", loadBalacerConfig.getId()), loadBalacerDto);
        return loadBalacerDto;
    }
    @Caching(evict = {
            @CacheEvict(value = "loadbalancerByTenant", key = "#loadBalancerDto.tenantId"),
            @CacheEvict(value = "loadbalancers", key = "#loadBalancerDto.tenantId + ':' + #loadBalancerDto.id")
    })
    @Transactional
    public LoadBalancerDto updateLoadBalancer(LoadBalancerDto loadBalancerDto) throws JsonProcessingException {
        LoadBalacerConfig existing = lbRepository.findByIdAndTenantId(loadBalancerDto.getId(), loadBalancerDto.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("LoadBalancer not found"));

        String existingSecret = existing.getUrlSecret();
        Instant existingDateCreated = existing.getDateCreated();

        LoadBalacerConfig updated = mapper.toConfig(loadBalancerDto);
        updated.setUrlSecret(existingSecret);
        updated.setDateCreated(existingDateCreated);
        if (loadBalancerDto.getId() != null && !loadBalancerDto.getId().isEmpty()) {
            updated.setId(loadBalancerDto.getId());
        }
        updated = lbRepository.save(updated);
        LoadBalancerDto result = mapper.toDto(updated);
        valkeyTemplate.opsForValue().set(String.format("lb:%s:details", updated.getId()), result);
        return result;
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
        return mapper.toDtos (lbRepository.findByTenantIdOrderByDateCreatedDesc(tenantId));
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

    @Transactional
    public void deleteLoadBalancers(String tenantId, List<String> ids) {
        lbRepository.deleteAllById(ids);
    }
}
