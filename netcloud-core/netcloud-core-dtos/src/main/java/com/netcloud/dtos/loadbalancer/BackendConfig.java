package com.netcloud.dtos.loadbalancer;

import com.netcloud.enums.RoutingProtocol;
import lombok.Data;

@Data
public class BackendConfig {
    private int port;
    private RoutingProtocol routingProtocol;
}
