package com.netcloud.dtos.servers;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ServerState {
    ACTIVE("active"),
    DEAD("dead"),
    RECOVERING("recovering");
    private String stateName;
}
