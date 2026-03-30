package auth.pymes.common.models.mappers;

import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMapper {
    
    default AuthResponse toAuthResponse(String accessToken, String refreshToken, UserEntityResponse user, TenantResponse tenant) {
        return new AuthResponse(accessToken, refreshToken, user, tenant);
    }
}
