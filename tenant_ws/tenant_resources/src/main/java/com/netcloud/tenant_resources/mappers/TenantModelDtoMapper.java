package com.netcloud.tenant_resources.mappers;

import com.netcloud.dtos.tenant.Tenant;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Mapper(componentModel = "spring")
public interface TenantModelDtoMapper {

    com.netcloud.models.tenant.Tenant toModel(Tenant model);

    Tenant toDto(com.netcloud.models.tenant.Tenant config);

    List<Tenant> toDtos(List<com.netcloud.models.tenant.Tenant> configs);

    List<com.netcloud.models.tenant.Tenant> toModels(List<Tenant> models);


}