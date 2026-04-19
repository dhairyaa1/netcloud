package com.netcloud.tenant_resources.resources;

import com.netcloud.dtos.tenant.Tenant;
import com.netcloud.tenant_resources.businesss.TenantBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantResource {

    @Autowired
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
}
