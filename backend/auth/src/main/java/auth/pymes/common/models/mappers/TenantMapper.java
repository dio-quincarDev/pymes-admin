package auth.pymes.common.models.mappers;

import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.entities.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TenantMapper {
    TenantResponse toResponse(Tenant entity);
}
