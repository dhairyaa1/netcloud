package com.netcloud.loadbalancer_ws.repositories;

import com.netcloud.models.loadbalancer.LoadBalacerConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LBRepository  extends MongoRepository<LoadBalacerConfig, String> {

    Optional<LoadBalacerConfig> findByIdAndTenantId(String id, String tenantId);

    List<LoadBalacerConfig> findByTenantIdOrderByDateCreatedDesc(String tenantId);
}