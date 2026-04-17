package auth.pymes.controller;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Users", description = "Endpoints de gestión de usuarios y perfil")
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.USERS_ROUTE)
public interface UserApi {

    @Operation(summary = "Obtener usuario actual", description = "Retorna los datos del usuario autenticado")
    @GetMapping(ApiPathConstants.USERS_ME)
    ResponseEntity<ApiResponse<UserEntityResponse>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User principal);
}
