package com.netcloud.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Secret {
    private String tenantId;
    private long interval;
    private String lbId;
}
