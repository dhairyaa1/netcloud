package com.netcloud.models.tenant;

import com.netcloud.models.loadbalancer.LoadBalacerConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.Instant;
import java.util.List;


@Getter
@AllArgsConstructor
@Document("tenant")
public class Tenant {
    private String tenantName;
    private String tenantId;
    @DocumentReference(lazy = true)
    @Indexed
    private List<LoadBalacerConfig> loadBalancerDtos;
    private Instant registeredAt;
}
