//package com.netcloud.loadbalancer.schedulers;
//
//import com.netcloud.commons.services.GCPService;
//import com.netcloud.commons.shared.LRUCache;
//import com.netcloud.loadbalancer.repositories.ServerRepository;
//import com.netcloud.models.loadbalancer.LoadBalacerConfig;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//
//@Service
//@RequiredArgsConstructor
//public class TenantDetailScheduler {
//    private LRUCache<String, LoadBalacerConfig> cache;
//    private final GCPService gcpService;
//    private final ServerRepository serverRepository;
//    RestTemplate restTemplate;
//
//    @Scheduled()
//    public void maintainCache() throws Exception {
//        String ipAddress = gcpService.getLocalIpAddress();
//        restTemplate.getForEntity(ipAddress);
//    }
//}
