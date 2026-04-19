package com.netcloud.tenant_resources.repository;

import com.netcloud.models.tenant.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TenantRepository extends MongoRepository<Tenant, String> {
    List<Tenant> findByAssignedServerIds(String serverId);
}
