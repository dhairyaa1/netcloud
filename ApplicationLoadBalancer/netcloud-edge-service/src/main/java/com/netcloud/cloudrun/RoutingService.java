package com.netcloud.cloudrun;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public HttpResponse<String> route(HttpServletRequest req, String ipAddress, int port) throws IOException, InterruptedException {
        String scheme = "http";
        String path = req.getRequestURI();
        String query = req.getQueryString();
        String targetUrl = String.format("%s://%s:%d%s%s",
                scheme, ipAddress, port, path,
                (query != null ? "?" + query : ""));

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(30));

        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String lower = name.toLowerCase();
            if (!lower.equals("host") && !lower.equals("content-length") && 
                !lower.equals("connection") && !lower.equals("transfer-encoding")) {
                Enumeration<String> values = req.getHeaders(name);
                while (values.hasMoreElements()) {
                    requestBuilder.header(name, values.nextElement());
                }
            }
        }

        String method = req.getMethod().toUpperCase();
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
            byte[] body = req.getInputStream().readAllBytes();
            requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        log.info("routing request to {}", targetUrl);
        return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }
}