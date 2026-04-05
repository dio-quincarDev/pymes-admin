package auth.pymes.controller;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Tenants", description = "Endpoints de gestión de empresas/tenants")
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE)
public interface TenantApi {

    @Operation(summary = "Obtener tenants del usuario", description = "Lista todas las empresas/tenants a las que pertenece el usuario")
    @GetMapping
    ResponseEntity<ApiResponse<Page<UserTenantResponse>>> getUserTenants(
            Pageable pageable,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Seleccionar tenant activo", description = "Cambia el tenant activo para el usuario")
    @PostMapping("/select")
    ResponseEntity<ApiResponse<AuthResponse>> selectTenant(
            @Valid @RequestBody SelectTenantRequest request,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Crear nuevo tenant", description = "Crea una nueva empresa/tenant para el usuario autenticado")
    @PostMapping
    ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal OAuth2User principal);
}
