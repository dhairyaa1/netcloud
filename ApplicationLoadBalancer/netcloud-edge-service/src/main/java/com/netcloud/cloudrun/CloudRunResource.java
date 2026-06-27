package com.netcloud.cloudrun;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.http.HttpResponse;

@RestController
@RequiredArgsConstructor
public class CloudRunResource {
    private final CloudRunService cloudRunService;

    @RequestMapping("/{tenantSecret}/**")
    public void route(
            @PathVariable String tenantSecret,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException, InterruptedException {
        
        HttpResponse<String> backendResponse = cloudRunService.route(request, tenantSecret);
        
        response.setStatus(backendResponse.statusCode());
        backendResponse.headers().map().forEach((name, values) ->
            values.forEach(value -> response.addHeader(name, value)));
        response.getOutputStream().write(backendResponse.body().getBytes());
    }
}