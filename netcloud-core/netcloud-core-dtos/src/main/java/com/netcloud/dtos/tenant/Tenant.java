package com.netcloud.dtos.tenant;

import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;


@Getter
@AllArgsConstructor
public class Tenant {
    private String tenantName;
    private String tenantId;
    private List<LoadBalancerDto> loadBalancerDtos;
    @Indexed
    private List<Server> servers;
}
