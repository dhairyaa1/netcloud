package com.netcloud.models.tenant;

import com.netcloud.models.loadbalancer.LoadBalancerConfig;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.Instant;
import java.util.List;


@Data
@Document("tenant")
public class Tenant {
    private String tenantName;
    private String company;
    private String phoneNumber;
    private String email;
    private String password;
    private String tenantId;
    @DocumentReference(lazy = true)
    @Indexed
    private List<LoadBalancerConfig> loadBalancerDtos;
    private Instant registeredAt;
}
