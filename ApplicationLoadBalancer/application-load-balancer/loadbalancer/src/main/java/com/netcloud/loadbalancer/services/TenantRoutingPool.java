//package com.netcloud.loadbalancer.services;
//
//import com.netcloud.dtos.Server;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentSkipListMap;
//
//public class TenantRoutingPool {
//    private final ConcurrentSkipListMap<Long, TreeMap<String, Server>> sortedConnections = new ConcurrentSkipListMap<>();
//
//    public TenantRoutingPool(List<Server> servers) {
//        for (Server server : servers) {
//            refreshServer(server);
//        }
//    }
//
//    public Server getBestServer() {
//        TreeMap<String, Server> map = sortedConnections.firstEntry().getValue();
//        return map.firstEntry().getValue();
//    }
//
//    public void refreshServer(Server server) {
//           long connectionCount = server.getConnectionCount();
//           TreeMap<String, Server> map = sortedConnections.get(connectionCount);
//           map.remove(server.getIpAddress());
//           sortedConnections.put(connectionCount, map);
//            map = sortedConnections.get(connectionCount + 1);
//            map.put(server.getIpAddress(), server);
//           sortedConnections.put(connectionCount + 1, map);
//    }
//}