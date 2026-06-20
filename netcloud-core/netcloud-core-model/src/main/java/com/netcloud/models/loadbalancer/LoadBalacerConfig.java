package com.netcloud.models.loadbalancer;

import com.netcloud.dtos.loadbalancer.BackendConfig;
import com.netcloud.dtos.loadbalancer.HealthCheckConfig;
import com.netcloud.enums.LoadBalancerType;
import com.netcloud.enums.RoutingAlgorithm;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "loadbalancer_config")
public class LoadBalacerConfig {
    private String loadBalancerName;
    private String urlSecret;
    @Id
    private String id;
    private String tenantId;
    private LoadBalancerType loadBalancerType;
    private Instant dateCreated;
    private RoutingAlgorithm routingAlgorithm;
    private BackendConfig backendConfig;
    private HealthCheckConfig healthCheckConfig;
}
