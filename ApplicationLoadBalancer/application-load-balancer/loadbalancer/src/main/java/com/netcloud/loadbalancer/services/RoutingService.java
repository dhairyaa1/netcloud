package com.netcloud.loadbalancer.services;

import com.netcloud.dtos.servers.ServerState;
import com.netcloud.loadbalancer.services.valkey.ValkeyConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class RoutingService {
    private final HttpClient httpClient;
    private RedisTemplate<String , String > valkey;
    public HttpResponse route(HttpServletRequest req, String ipAddress, int port, String tenantId, String lbId) throws IOException {
        String scheme = "http"; // Switch to "https" if backend TLS is enabled
        String path = req.getRequestURI();
        int index = path.indexOf("/", 1);
        path = path.substring(index);
        String query = req.getQueryString();
        String targetUrl = String.format("%s://%s:%s%s%s",
                scheme, ipAddress, port, path,
                (query != null ? "?" + query : ""));

        // 2. Build outbound request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(30)); // Matches §5 failure detection window

        // 3. Forward headers (filter hop-by-hop to prevent protocol corruption)
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (isForwardableRequestHeader(name)) {
                Enumeration<String> values = req.getHeaders(name);
                while (values.hasMoreElements()) {
                    requestBuilder.header(name, values.nextElement());
                }
            }
        }

        // Add standard proxy headers for tenant backend logging/debugging
        requestBuilder.header("X-Forwarded-For", req.getRemoteAddr());
        requestBuilder.header("X-Forwarded-Host", req.getServerName());
        requestBuilder.header("X-Forwarded-Proto", req.getScheme());

        // 4. Forward request body (only for methods that carry payloads)
        String method = req.getMethod().toUpperCase();
        if (Set.of("POST", "PUT", "PATCH").contains(method)) {
            byte[] bodyBytes = req.getInputStream().readAllBytes();
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }

        // 5. Execute request & stream backend response directly to tenant UI
            try {
                HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 500) {
                evictToDeadPool(tenantId, ipAddress, lbId);
            }
            return response;
        }
        catch(IOException | InterruptedException e){
            // Backend unreachable, timeout, or connection reset
            return null; // Triggers recordResponse(ip, false)
        }
    }
    private boolean isForwardableRequestHeader(String name) {
        String lower = name.toLowerCase();
        return !lower.equals("host") && !lower.equals("content-length") &&
                !lower.equals("connection") && !lower.equals("transfer-encoding") &&
                !lower.equals("keep-alive") && !lower.equals("upgrade") &&
                !lower.equals("te") && !lower.equals("trailer");
    }


    private void evictToDeadPool(String tenantId, String backend, String lbId) {

        log.warn("[LBServer] lbId={} evicted backend={} → dead pool queue.",
                lbId, backend);

        valkey.opsForValue().append(
                String.format(ValkeyConstants.getKey(ServerState.ACTIVE, lbId)),
                backend
        );
    }


}
