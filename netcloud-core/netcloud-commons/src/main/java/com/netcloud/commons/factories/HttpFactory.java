package com.netcloud.commons.factories;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestTemplate;

public class HttpFactory extends RestTemplate{
    public void forward(HttpHeaders headers,
                        @RequestBody String body,
                        HttpMethod method, String serverName)
    {
        exchange(serverName, method,  new HttpEntity<>(body, headers), byte[].class);
    }
}
