package com.netcloud.dtos.loadbalancer;

import com.netcloud.enums.LoadBalancerType;
import com.netcloud.enums.RoutingAlgorithm;
import lombok.Data;

@Data
public class LbConfig {
    private LoadBalancerType loadBalancerType;
    private RoutingAlgorithm routingAlgorithm;
    private HealthCheckConfig healthCheckConfig;
    private BackendConfig backendConfig;
}
