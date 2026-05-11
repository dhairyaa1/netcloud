package com.netcloud.control_plane.resources;

import com.netcloud.commons.SecretGenerator;
import com.netcloud.commons.dto.Secret;
import com.netcloud.control_plane.services.CacheService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.netcloud.control_plane.utils.HostUtil;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class AutodiscoveryResource {

    private final CacheService cacheService;

    private final SecretGenerator secretGenerator;
    @GetMapping("{tenantSecret}")
    public void ping(HttpServletRequest request, @PathVariable String tenantSecret)
    {
        String ipaddress = HostUtil.getClientIp(request);
        Secret secret = secretGenerator.getClaimsFromSecret(tenantSecret);
        log.info("Request recieved from ipaddress {}, for lbid {}, tenantid {}", ipaddress, secret.getLbId(), secret.getTenantId());
        cacheService.markAsActive(secret.getLbId(), ipaddress);
        log.info("Marking ipaddress as active");
    }
}
