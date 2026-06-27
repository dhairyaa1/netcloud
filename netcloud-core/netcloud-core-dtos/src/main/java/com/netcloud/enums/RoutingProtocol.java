package com.netcloud.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RoutingProtocol {
    HTTP("http"),
    HTTPS("https");
    private final String name;
}
