package com.netcloud.loadbalancer_ws.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netcloud.commons.SecretGenerator;
import com.netcloud.commons.dto.Secret;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.loadbalancer_ws.business.LoadBalancerConfigService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/lb")
public class LoadBalancerConfigResource {

    private final LoadBalancerConfigService loadBalancerConfigService;
    private final SecretGenerator secretGenerator;
    @PostMapping("/save")
    public LoadBalancerDto addLoadbalancer(@RequestBody LoadBalancerDto lbConfig, @RequestHeader("tenant-id") String tenantID) throws JsonProcessingException {
        lbConfig.setTenantId(tenantID);
       return loadBalancerConfigService.createLoadBalancer(lbConfig);
    }
    @PutMapping
    public LoadBalancerDto updateLoadbalancer(@RequestBody LoadBalancerDto lbConfig, @RequestHeader("tenant-id") String tenantID) throws JsonProcessingException {
        lbConfig.setTenantId(tenantID);
        return loadBalancerConfigService.createLoadBalancer(lbConfig);
    }

    @GetMapping
    public List<LoadBalancerDto> getLoadBalancers(@RequestHeader("tenant-id") String tenantId)
    {
        return loadBalancerConfigService.getAllByTenant(tenantId);
    }
    @GetMapping("/interval/{tenantSecret}")
    public long getInterval(@PathVariable String tenantSecret)
    {
        return secretGenerator.getClaimsFromSecret(tenantSecret).getInterval();
    }
    @GetMapping("/{lbId}")
    public LoadBalancerDto getLoadBalancerByTenantAndId(@RequestHeader("tenant-id") String tenantId, @PathVariable String lbId)
    {
        return loadBalancerConfigService.getLoadBalancer(tenantId, lbId);
    }

    @GetMapping("/tenant/{tenantSecret}")
    public Secret getTenantIdAnd(@PathVariable String tenantSecret)
    {
        return loadBalancerConfigService.getSecret(tenantSecret);
    }
    @DeleteMapping("/{lbId}")
    public void delete(@RequestHeader("tenant-id") String tenantId, @PathVariable String lbId)
    {
        loadBalancerConfigService.deleteLoadBalancer(tenantId, lbId);
    }
}
