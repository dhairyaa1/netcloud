package com.netcloud.control_plane.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties
public class ControlPlaneProperties {
    private String instanceId = "few21451dv";
    private int totalNodes = 1;
    private long healthCheckInterval = 5;
    private int recoveryThreshold = 4;
    private long healthCheckTimeout = 5;
    private long deadPoolBaseInterval = 30;
    private long deadPoolMaxInterval = 300;
    private double backoffMultiplier = 2.0;
}