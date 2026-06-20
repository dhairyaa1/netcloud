package com.netcloud.control_plane.valkey;

import com.netcloud.commons.enums.CacheEventTypes;
import com.netcloud.control_plane.services.HealthChecker;
import com.netcloud.dtos.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValkeyDeadpoolListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final HealthChecker healthChecker;
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        try {
            Server server = objectMapper.readValue(payload, Server.class);
            if (!CacheEventTypes.MOVED_TO_DEADPOOL.toString().equalsIgnoreCase(server.getEventType())) {
                return;
            }
            log.info("New server recieved in deadpool {}", server);
            String serverKey = server.getLbId() + ":" + server.getServerIp();
            healthChecker.startMonitoringIfAssigned(serverKey, server);
        } catch (Exception e) {
            log.error("Failed to parse deadpool event: {}", payload, e);
        }
    }
}