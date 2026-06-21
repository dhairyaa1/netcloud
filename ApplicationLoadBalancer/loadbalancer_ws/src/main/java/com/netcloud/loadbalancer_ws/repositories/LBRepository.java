package com.netcloud.loadbalancer_ws.repositories;

import com.netcloud.models.loadbalancer.LoadBalancerConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LBRepository  extends MongoRepository<LoadBalancerConfig, String> {

    Optional<LoadBalancerConfig> findByIdAndTenantId(String id, String tenantId);

    List<LoadBalancerConfig> findByTenantIdOrderByDateCreatedDesc(String tenantId);
}