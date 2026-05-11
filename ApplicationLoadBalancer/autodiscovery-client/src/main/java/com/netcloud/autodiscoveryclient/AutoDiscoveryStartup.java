package com.netcloud.autodiscoveryclient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AutoDiscoveryStartup {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${netcloud.autodiscovery.urls}")
    private List<String> autodiscoveryUrls;
    private String url;

    @Autowired
    private ScheduledExecutorService retryExecutor;


    @PostConstruct
    public void start() {
        if (autodiscoveryUrls == null || autodiscoveryUrls.isEmpty()) {
            return;
        }

        for (String url : autodiscoveryUrls) {
            ping(url);
        }
    }

    private void ping(String url) {
        try {
            this.url = url;
            this.safePing();
        } catch (Exception e) {
            System.err.println("Could not ping the loadbalancer for " + url + ": " + e.getMessage());
        }
    }

    private void safePing() {
        try {
            System.out.println(Thread.currentThread().getName() + " | Pinging: " + url);
            restTemplate.getForEntity(url, String.class);
            System.out.println("Pinged successfully");
        } catch (Exception e) {
            System.err.println("Heartbeat failed for " + url);
            scheduleRetry();
        }
    }


    /** Retry the one-time registration ping if the first attempt failed. */
    private void scheduleRetry() {
        retryExecutor.schedule(this::safePing, 5, TimeUnit.SECONDS);
    }
}