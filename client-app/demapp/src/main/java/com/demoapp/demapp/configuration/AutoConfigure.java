package com.demoapp.demapp.configuration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import com.netcloud.autodiscoveryclient.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


@Configuration
public class AutoConfigure {

    @Bean
    public AutoDiscoveryStartup autoDiscoveryStartup()
    {
        return new AutoDiscoveryStartup();
    }
}