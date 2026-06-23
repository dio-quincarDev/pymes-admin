package auth.pymes.common.models.mappers;

import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "plan", ignore = true)
    UserEntityResponse toResponse(UserEntity entity);
}
