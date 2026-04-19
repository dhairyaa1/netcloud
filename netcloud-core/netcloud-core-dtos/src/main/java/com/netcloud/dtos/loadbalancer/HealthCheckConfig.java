package com.netcloud.dtos.loadbalancer;

import com.netcloud.enums.RoutingProtocol;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HealthCheckConfig implements Serializable {
    private int interval;
    private String endpoint;
    private int timeout;
}
