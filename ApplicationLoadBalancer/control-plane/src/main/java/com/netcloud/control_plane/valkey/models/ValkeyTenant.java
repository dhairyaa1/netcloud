package com.netcloud.control_plane.valkey.models;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class ValkeyTenant {
    List<String> tenantIds;
    Map<String, List<String>> deadpool;
}
