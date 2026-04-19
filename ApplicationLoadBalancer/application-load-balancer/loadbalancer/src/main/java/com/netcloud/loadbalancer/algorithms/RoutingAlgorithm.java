package com.netcloud.loadbalancer.algorithms;


import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.http.HttpResponse;

public interface RoutingAlgorithm {
    HttpResponse<byte[]> route(HttpServletRequest request, LoadBalancerDto loadBalancerDto) throws InterruptedException, IOException;
}
