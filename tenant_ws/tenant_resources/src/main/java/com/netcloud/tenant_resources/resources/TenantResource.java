package com.netcloud.tenant_resources.resources;

import com.netcloud.dtos.tenant.LoginResponse;
import com.netcloud.dtos.tenant.Tenant;
import com.netcloud.tenant_resources.businesss.TenantBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantResource {

    private final TenantBusiness tenantBusiness;
    @GetMapping("/getTenant")
    public Tenant tenant(@RequestParam("tenant-id") String tenantId)
    {
        return tenantBusiness.getTenantById(tenantId);
    }
    @PostMapping("/save")
    public Tenant saveTenantInfo(@RequestBody Tenant tenant)
    {
        return tenantBusiness.saveTenant(tenant);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody Map<String, String> credentials) {
        return tenantBusiness.login(credentials.get("email"), credentials.get("password"));
    }

    @PostMapping("/register")
    public LoginResponse register(@RequestBody Tenant tenant) {
        return tenantBusiness.registerTenant(tenant);
    }
}
