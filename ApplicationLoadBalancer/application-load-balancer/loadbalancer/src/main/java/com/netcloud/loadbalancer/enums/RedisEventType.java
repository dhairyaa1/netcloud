package com.netcloud.loadbalancer.enums;

import lombok.Getter;

@Getter
public enum RedisEventType {
    SET("set"),
    EXPIRE("expire"),
    EXPIRED("expired"),
    DEL("del");

    private String value;
    RedisEventType(String value)
    {
        this.value = value;
    }
}
