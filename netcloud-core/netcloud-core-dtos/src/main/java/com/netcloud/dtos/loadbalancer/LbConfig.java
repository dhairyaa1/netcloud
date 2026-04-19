package com.netcloud.dtos.loadbalancer;

import com.netcloud.enums.LoadBalancerType;
import com.netcloud.enums.RoutingPoliciesEnum;
import com.netcloud.enums.RoutingProtocol;
import lombok.Data;

@Data
public class LbConfig {
    private LoadBalancerType loadBalancerType;
    private RoutingProtocol routingProtocol;
    private RoutingPoliciesEnum routingAlgorithm;
    private HealthCheckConfig healthCheckConfig;
    private BackendConfig backendConfig;
    private String tenantId;
}
