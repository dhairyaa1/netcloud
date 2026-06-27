package com.netcloud.loadbalancer_ws.repositories;

import com.netcloud.dtos.Routes;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoutingRepository extends MongoRepository<Routes, String> {
    void deleteByServer(String server);
}
