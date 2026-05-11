package com.netcloud.tenant_resources.businesss;

import com.netcloud.dtos.tenant.LoginResponse;
import com.netcloud.dtos.tenant.Tenant;
import com.netcloud.tenant_resources.services.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantBusiness {
    private final TenantService tenantService;
    public Tenant getTenantById(String tenantID)
    {
        return tenantService.getTenantDetailsById(tenantID);
    }
    public Tenant saveTenant(Tenant tenant)
    {
        return tenantService.addTenantDetails(tenant);
    }

    public LoginResponse login(String email, String password) {
        return tenantService.login(email, password);
    }

    public LoginResponse registerTenant(Tenant tenant) {
        return tenantService.registerTenant(tenant);
    }
}
