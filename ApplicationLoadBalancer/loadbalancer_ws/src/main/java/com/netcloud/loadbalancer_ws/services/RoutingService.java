package com.netcloud.loadbalancer_ws.services;

import com.netcloud.dtos.Routes;
import com.netcloud.loadbalancer_ws.repositories.RoutingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingService {
    public final RoutingRepository routingRepository;

    public List<Routes> getRoutes()
    {
        return routingRepository.findAll();
    }
    public void deleteRoute(String serverID)
    {
        routingRepository.deleteByServer(serverID);
    }
    public void addRoute(Routes route)
    {
        routingRepository.save(route);
    }
}
