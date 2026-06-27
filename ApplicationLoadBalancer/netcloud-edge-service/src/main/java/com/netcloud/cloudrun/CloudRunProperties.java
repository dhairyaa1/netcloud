package com.netcloud.cloudrun;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "netcloud.cloudrun")
@Data
public class CloudRunProperties {
    private int serversPerTenant = 3;
    private int replicas = 2;
}
