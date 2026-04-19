//package com.netcloud.loadbalancer.services;
//import com.netcloud.dtos.servers.Server;
//import com.netcloud.loadbalancer.configuration.GlobalCache;
//import com.netcloud.loadbalancer.enums.RedisEventType;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.connection.Message;
//import org.springframework.data.redis.connection.MessageListener;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//
//@Slf4j
//@Service
//public class RedisKeyspaceSubscriber implements MessageListener {
//
//    private GlobalCache cache;
//    @Override
//    public void onMessṣage(Message message, byte[] pattern) {
//        String channel = new String(message.getChannel());
//        String action = channel.substring(channel.lastIndexOf(":") + 1);
//        String[] properties = message.toString().split("\\|\\|");
//        String tenantID = properties[0];
//        String ipAddress = properties[1];
//        LinkedList<String> ipList = cache.getLocalCache().get(tenantID).orElse(new LinkedList<>());
//        cache.getRoutingResourceMap().computeIfAbsent(tenantID, k -> new TenantRoutingPool(new ArrayList<>()));
//
//        if(action.equals(RedisEventType.SET.getValue()))
//        {
//            ipList.add(ipAddress);
//            Server server = new Server();
//            server.setIpAddress(ipAddress);
//            cache.getRoutingResourceMap().get(tenantID).refreshServer(server);
//            cache.getLocalCache().put(tenantID, ipList);
//            log.info("Adding ipaddress to localcache");
//        }
//        else if(action.equals(RedisEventType.EXPIRED.getValue())){
//            ipList.remove(ipAddress);
//            cache.getLocalCache().put(tenantID, ipList);
//            log.info("Removing ipaddress to localcache");
//        }
//    }
//}