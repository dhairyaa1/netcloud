package com.netcloud.tenant_resources;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.netcloud")
public class TenantWsApplication {
    public static void main(String[] args) {
        SpringApplication.run(TenantWsApplication.class, args);
    }
}
