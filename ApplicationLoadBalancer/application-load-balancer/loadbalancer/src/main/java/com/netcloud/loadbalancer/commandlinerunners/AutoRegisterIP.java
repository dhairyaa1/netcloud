//package com.netcloud.loadbalancer.commandlinerunners;
//
//import com.netcloud.commons.services.GCPService;
//import com.netcloud.loadbalancer.repositories.ServerRepository;
//import com.netcloud.models.common.Server;
//import jakarta.annotation.PreDestroy;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class AutoRegisterIP implements CommandLineRunner {
//
//    private final GCPService gcpService;
//    private final ServerRepository serverRepository;
//    @Override
//    public void run(String... args) throws Exception {
//        String ipAddress = gcpService.getLocalIpAddress();
//        String zone = gcpService.fetchGcpZone();
//        if (!serverRepository.findByIpAddress(ipAddress).isPresent()) {
//            Server server = Server.builder()
//                    .ipAddress(ipAddress) // Set IP as a field
//                    .zone(zone)
//                    .build();
//            serverRepository.save(server);
//        }
//    }
//
//}
