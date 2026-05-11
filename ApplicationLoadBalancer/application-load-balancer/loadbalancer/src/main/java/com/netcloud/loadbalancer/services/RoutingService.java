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
    public HttpResponse route(HttpServletRequest req, String ipAddress, int port, byte[] bodyBytes) throws IOException {
        String scheme = "http"; // Switch to "https" if backend TLS is enabled
        String path = req.getRequestURI();
        int index = path.indexOf("/", 1);
        path = path.substring(index);
        String query = req.getQueryString();
        String targetUrl = String.format("%s://%s:%s%s%s",
                scheme, ipAddress, port, path,
                (query != null ? "?" + query : ""));
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(30)); // Matches §5 failure detection window
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

        requestBuilder.header("X-Forwarded-For", req.getRemoteAddr());
        requestBuilder.header("X-Forwarded-Host", req.getServerName());
        requestBuilder.header("X-Forwarded-Proto", req.getScheme());

        String method = req.getMethod().toUpperCase();
        if (Set.of("POST", "PUT", "PATCH").contains(method)) {
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(bodyBytes));
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        log.info("Routing request to the url : {}", targetUrl);
        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            return response;
        }
        catch(IOException | InterruptedException e){
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
