package com.netcloud.tenant_resources.resources;

import com.netcloud.commons.SecretGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TenantSecretResource {

    @Autowired
    private SecretGenerator secretGenerator;
    @GetMapping("{tenantSecret}")
    public long getSecret(@PathVariable String tenantSecret)
    {
        return secretGenerator.getClaimsFromSecret(tenantSecret).getInterval();
    }
}
