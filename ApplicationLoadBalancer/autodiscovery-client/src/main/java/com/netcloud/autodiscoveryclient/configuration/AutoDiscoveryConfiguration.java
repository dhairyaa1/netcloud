package com.netcloud.autodiscoveryclient.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@AutoConfiguration
public class AutoDiscoveryConfiguration {
    @Bean
    public RestTemplate restTemplate()
    {
        return new RestTemplate();
    }
}
