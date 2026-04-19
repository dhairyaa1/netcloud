package com.netcloud.loadbalancer.services.valkey;


import com.netcloud.dtos.servers.ServerState;

public class ValkeyConstants {
    public static String getKey(ServerState type, String lbId) { return "lb:" + lbId + ":" + type.name(); }

}
