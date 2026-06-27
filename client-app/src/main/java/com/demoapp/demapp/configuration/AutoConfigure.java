package com.demoapp.demapp.configuration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import com.netcloud.autodiscoveryclient.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@Configuration
public class AutoConfigure {

    @Bean
    public AutoDiscoveryStartup autoDiscoveryStartup()
    {
        return new AutoDiscoveryStartup();
    }

    @Bean
    public ScheduledExecutorService retryExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}