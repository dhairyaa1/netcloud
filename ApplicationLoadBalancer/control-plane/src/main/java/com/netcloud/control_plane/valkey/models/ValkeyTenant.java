package com.netcloud.control_plane.valkey.models;

import lombok.Data;
import lombok.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Data
@Value
public class ValkeyTenant {
    List<String> tenantIds;
    Map<String, List<String>> deadpool;
}
