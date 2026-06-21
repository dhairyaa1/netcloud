package com.netcloud.loadbalancer_ws.services;

import com.netcloud.loadbalancer_ws.config.ApplicationConfiguration;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

@Service
public class CustomMappingService {

    private final ApplicationConfiguration config;

    public CustomMappingService(ApplicationConfiguration config) {
        this.config = config;
    }
    @Named("getLbUrl")
    public String getLBUrl(String secret) {
        return config.getLbUrl() + "/" + secret;
    }
    @Named("getDiscoveryUrl")
    public String getDiscoveryUrl(String secret) {
        return config.getAutodiscoveryUrl() + "/" + secret;
    }

}
