package com.netcloud.cloudrun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CloudRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudRunApplication.class, args);
    }
}