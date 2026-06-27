package com.netcloud.dtos.tenant;

import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.List;


@Data
public class Tenant {
    private String tenantName;
    private String company;
    private String phoneNumber;
    private String email;
    private String password;
    private String tenantId;
    private List<LoadBalancerDto> loadBalancerDtos;
}
