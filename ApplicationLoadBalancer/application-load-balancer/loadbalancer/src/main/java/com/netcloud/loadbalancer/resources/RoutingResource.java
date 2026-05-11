package com.netcloud.loadbalancer.resources;

import com.netcloud.commons.SecretGenerator;
import com.netcloud.commons.dto.Secret;
import com.netcloud.loadbalancer.business.RoutingBAL;
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
        try {
            log.info("Started the request");
            Secret secret = secretGenerator.getClaimsFromSecret(tenantSecret);
            HttpResponse<String> response = (HttpResponse<String>) routingBAL.route(request, secret);
            servletResponse.setStatus(response.statusCode());
            response.headers().map().forEach((name, values) ->
                    values.forEach(value -> servletResponse.addHeader(name, value)));
            servletResponse.getOutputStream().write(response.body().getBytes());
        }
        catch(Exception e)
        {
            log.info("Message is {} exception is {}",e.getMessage(), e.getStackTrace());
        }
    }
    @GetMapping("/")  // Cloud Run probes "/" by default
    public String ready() {
        return "ok";
    }
}