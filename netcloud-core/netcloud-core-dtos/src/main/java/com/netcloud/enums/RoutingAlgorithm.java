package com.netcloud.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoutingAlgorithm {
    ROUND_ROBIN(0),
    LEAST_CONNECTION(1),
    WEIGHTED_ROUND_ROBIN(2),
    ANT_COLONY(3),
    IP_HASH(4),
    WEIGHTED_LEAST_CONNECTION(5);

    private final int value;

    RoutingAlgorithm(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RoutingAlgorithm fromValue(String value) {
        try {
            int intVal = Integer.parseInt(value);
            for (RoutingAlgorithm algo : values()) {
                if (algo.value == intVal) {
                    return algo;
                }
            }
        } catch (NumberFormatException e) {
            return RoutingAlgorithm.valueOf(value);
        }
        throw new IllegalArgumentException("Unknown routing algorithm: " + value);
    }
}
