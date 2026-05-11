package com.netcloud.loadbalancer_ws.mappers;

import com.netcloud.dtos.loadbalancer.LbConfig;
import com.netcloud.dtos.loadbalancer.LoadBalancerDto;
import com.netcloud.loadbalancer_ws.services.CustomMappingService;
import com.netcloud.models.loadbalancer.LoadBalacerConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = CustomMappingService.class)
public interface LBModelDtoMapper {

    @Mapping(source = "name", target = "loadBalancerName")
    @Mapping(target = "id", ignore = true)
    LoadBalacerConfig toConfig(LoadBalancerDto model);

    @Mapping(source = "loadBalancerName", target = "name")
    @Mapping(source = "urlSecret", target = "lbUrl", qualifiedByName = "getLbUrl")
    @Mapping(source = "urlSecret", target = "discoveryUrl", qualifiedByName = "getDiscoveryUrl")
    LoadBalancerDto toDto(LoadBalacerConfig config);

    List<LoadBalancerDto> toDtos(List<LoadBalacerConfig> configs);

    // Reverse List Mapping
    List<LoadBalacerConfig> toConfigs(List<LoadBalancerDto> models);
    LbConfig toLbCacheConfig(LoadBalancerDto loadBalancerDto);

}