package com.netcloud.dtos.loadbalancer;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Data
public class LoadBalancerDto extends LbConfig implements Serializable {
    private String name;
    private String lbUrl;
    private String discoveryUrl;
    private String id;
    @Setter
    private String tenantId;
    private Date dateCreated;


}
