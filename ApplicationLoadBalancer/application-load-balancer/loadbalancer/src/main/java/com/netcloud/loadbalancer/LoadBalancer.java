package com.netcloud.loadbalancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.netcloud")
public class LoadBalancer {
    public static void main(String[] args) {
        SpringApplication.run(LoadBalancer.class, args);
    }
}