package com.netcloud.dtos.servers;

import com.netcloud.enums.RoutingProtocol;
import lombok.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
@AllArgsConstructor
public class Server {
    private String lbId;
    private String tenantId;
    private String serverIp;
    private String healthUrl;
    private String eventType;
    private int healthTimeout;
    private int backendPort;
    AtomicLong connectionCount;
    long cpuUtilisation;
    long memoryUtilisation;
    int consecutiveSuccesses;
    int consecutiveFailures;
    double trafficWeight;
    RoutingProtocol routingProtocol;
    private String status;
    private Instant lastChecked;
    private Instant timestamp;
    private int healthCheckInterval;
    ServerState serverState;
    public void incConnections() { connectionCount.incrementAndGet(); }
    public void decConnections() { connectionCount.decrementAndGet(); }
    public int incFailures() { consecutiveSuccesses = 0; return ++this.consecutiveFailures; }
    public void resetFailures() { consecutiveFailures = 0; }
    public int incSuccesses() {consecutiveFailures = 0; return ++consecutiveSuccesses; }
    public void resetSuccesses() { consecutiveSuccesses = 0; }
    public void resetConnections(){connectionCount.set(0);}
}
