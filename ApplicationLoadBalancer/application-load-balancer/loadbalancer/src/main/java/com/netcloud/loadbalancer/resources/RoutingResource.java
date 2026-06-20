package com.netcloud.loadbalancer.resources;

import com.netcloud.commons.SecretGenerator;
import com.netcloud.commons.dto.Secret;
import com.netcloud.loadbalancer.business.RoutingBAL;
import com.netcloud.loadbalancer.exceptions.BackendUnavailableException;
import com.netcloud.loadbalancer.exceptions.SecretValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.http.HttpResponse;

@RestController
@Slf4j
@RequiredArgsConstructor
public class RoutingResource {

    private final RoutingBAL routingBAL;
    private final SecretGenerator secretGenerator;

    @RequestMapping("/{tenantSecret}/**")
    public void route(
            @PathVariable String tenantSecret,
            HttpServletRequest request,
            HttpServletResponse servletResponse) throws IOException, InterruptedException {
        log.info("Started the request, recieved by lb instance");
        Secret secret;
        try {
            secret = secretGenerator.getClaimsFromSecret(tenantSecret);
        } catch (RuntimeException e) {
            throw new SecretValidationException("Invalid or tampered secret provided", e);
        }
        HttpResponse<String> response = (HttpResponse<String>) routingBAL.route(request, secret);
        if (response == null) {
            throw new BackendUnavailableException("Backend servers are currently unavailable");
        }
        servletResponse.setStatus(response.statusCode());
        response.headers().map().forEach((name, values) ->
                values.forEach(value -> servletResponse.addHeader(name, value)));
        servletResponse.getOutputStream().write(response.body().getBytes());
    }
    @GetMapping("/")  // Cloud Run probes "/" by default
    public String ready() {
        return "ok";
    }
}