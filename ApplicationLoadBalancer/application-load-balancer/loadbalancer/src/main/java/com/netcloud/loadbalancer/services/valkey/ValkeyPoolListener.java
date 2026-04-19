package com.netcloud.loadbalancer.services.valkey;

import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class ValkeyPoolListener  implements MessageListener {
    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {


    }
}
