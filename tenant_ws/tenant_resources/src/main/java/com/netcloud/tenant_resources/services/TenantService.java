package com.netcloud.tenant_resources.services;


import com.netcloud.dtos.tenant.Tenant;
import com.netcloud.tenant_resources.exceptions.TenantNotFound;
import com.netcloud.tenant_resources.mappers.TenantModelDtoMapper;
import com.netcloud.tenant_resources.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TenantService {

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    TenantModelDtoMapper tenantModelDtoMapper;

    public Tenant getTenantDetailsById(String id)
    {
        Optional<com.netcloud.models.tenant.Tenant> tenant = tenantRepository.findById(id);
        if(tenant.isPresent()) {
            return tenantModelDtoMapper.toDto(tenant.get());
        }
        else {
            throw new TenantNotFound();
        }
    }
    public Tenant addTenantDetails(Tenant tenant)
    {
        return tenantModelDtoMapper.toDto(tenantRepository.save(
                tenantModelDtoMapper.toModel(tenant)));
    }
}
