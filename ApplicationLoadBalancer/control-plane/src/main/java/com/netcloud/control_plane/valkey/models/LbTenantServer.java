package com.netcloud.control_plane.valkey.models;

import lombok.Data;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Data
public class LbTenantServer {
    private String lbId;
    private String tenantId;
}
