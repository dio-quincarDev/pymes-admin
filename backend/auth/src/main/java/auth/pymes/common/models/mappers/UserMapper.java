package auth.pymes.common.models.mappers;

import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserEntityResponse toResponse(UserEntity entity);
}
