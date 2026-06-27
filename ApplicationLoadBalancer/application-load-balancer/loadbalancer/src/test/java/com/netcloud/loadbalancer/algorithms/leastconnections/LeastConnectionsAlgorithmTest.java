package com.netcloud.loadbalancer.algorithms.leastconnections;

import com.netcloud.dtos.loadbalancer.BackendConfig;
import com.netcloud.dtos.loadbalancer.HealthCheckConfig;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.dtos.servers.Server;
import com.netcloud.enums.RoutingProtocol;
import com.netcloud.loadbalancer.exceptions.BackendUnavailableException;
import com.netcloud.loadbalancer.services.RoutingService;
import com.netcloud.loadbalancer.services.valkey.ValkeyPoolManager;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeastConnectionsAlgorithmTest {

    @Mock
    private RoutingService routingService;

    @Mock
    private ValkeyPoolManager poolManager;

    @Mock
    private HttpServletRequest request;

    private LeastConnectionsAlgorithm algorithm;

    @BeforeEach
    void setUp() throws IOException {
        algorithm = new LeastConnectionsAlgorithm(routingService, poolManager);
        ServletInputStream sis = new ServletInputStream() {
            private final ByteArrayInputStream bis = new ByteArrayInputStream(new byte[0]);
            @Override public int read() { return bis.read(); }
            @Override public boolean isFinished() { return true; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(jakarta.servlet.ReadListener listener) {}
        };
        lenient().when(request.getInputStream()).thenReturn(sis);
        lenient().when(request.getRequestURI()).thenReturn("/secret/path");
        lenient().when(request.getQueryString()).thenReturn(null);
        lenient().when(request.getMethod()).thenReturn("GET");
        lenient().when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getServerName()).thenReturn("localhost");
        lenient().when(request.getScheme()).thenReturn("http");
    }

    private Server createServer(String ip, long connections) {
        return Server.builder()
                .serverIp(ip)
                .connectionCount(new AtomicLong(connections))
                .build();
    }

    private LoadBalancerDto createLbDto(String tenantId) {
        LoadBalancerDto dto = new LoadBalancerDto();
        dto.setTenantId(tenantId);
        dto.setId("lb-1");
        BackendConfig backend = new BackendConfig();
        backend.setPort(8080);
        backend.setRoutingProtocol(RoutingProtocol.HTTP);
        dto.setBackendConfig(backend);
        HealthCheckConfig health = new HealthCheckConfig();
        health.setTimeout(30);
        dto.setHealthCheckConfig(health);
        return dto;
    }

    @Test
    void shouldPickServerWithLeastConnections() throws Exception {
        Server busy = createServer("10.0.0.1", 10);
        Server idle = createServer("10.0.0.2", 1);
        LoadBalancerDto lbDto = createLbDto("tenant-1");
        HttpResponse mockResponse = mock(HttpResponse.class);

        when(poolManager.selectServers("lb-1")).thenReturn(List.of(busy, idle));
        when(routingService.route(any(), eq("10.0.0.2"), eq(8080), any(), eq("http"), eq(30)))
                .thenReturn(mockResponse);

        HttpResponse result = algorithm.route(request, lbDto);

        assertNotNull(result);
        verify(poolManager).incrementConnections(idle);
        verify(poolManager).decrementConnections(idle);
    }

    @Test
    void shouldRouteSuccessfullyWithSingleServer() throws Exception {
        Server server = createServer("10.0.0.1", 0);
        LoadBalancerDto lbDto = createLbDto("tenant-2");
        HttpResponse mockResponse = mock(HttpResponse.class);

        when(poolManager.selectServers("lb-1")).thenReturn(List.of(server));
        when(routingService.route(any(), eq("10.0.0.1"), eq(8080), any(), eq("http"), eq(30)))
                .thenReturn(mockResponse);

        HttpResponse result = algorithm.route(request, lbDto);

        assertNotNull(result);
        verify(poolManager).recordResponse(server, true, "lb-1");
    }

    @Test
    void shouldRetryOnFailureAndSucceed() throws Exception {
        Server server1 = createServer("10.0.0.1", 5);
        Server server2 = createServer("10.0.0.2", 3);
        LoadBalancerDto lbDto = createLbDto("tenant-3");
        HttpResponse mockResponse = mock(HttpResponse.class);

        when(poolManager.selectServers("lb-1")).thenReturn(List.of(server1, server2));
        when(routingService.route(any(), eq("10.0.0.2"), eq(8080), any(), eq("http"), eq(30)))
                .thenReturn(null);
        when(routingService.route(any(), eq("10.0.0.1"), eq(8080), any(), eq("http"), eq(30)))
                .thenReturn(mockResponse);

        HttpResponse result = algorithm.route(request, lbDto);

        assertNotNull(result);
        verify(poolManager, times(1)).recordResponse(any(), eq(false), eq("lb-1"));
        verify(poolManager).recordResponse(any(), eq(true), eq("lb-1"));
    }

    @Test
    void shouldThrowWhenAllServersFail() throws Exception {
        Server server = createServer("10.0.0.1", 0);
        LoadBalancerDto lbDto = createLbDto("tenant-4");

        when(poolManager.selectServers("lb-1")).thenReturn(List.of(server));
        when(routingService.route(any(), anyString(), eq(8080), any(), eq("http"), eq(30)))
                .thenReturn(null);

        assertThrows(BackendUnavailableException.class, () -> algorithm.route(request, lbDto));
    }
}
