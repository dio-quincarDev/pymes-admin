package auth.pymes.controller;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.common.models.dto.request.AcceptInvitationRequest;
import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Authentication", description = "Endpoints de autenticación y gestión de usuarios")
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.AUTH_ROUTE)
public interface AuthApi {

    @Operation(summary = "Registro de usuario local", description = "Crea un usuario, su empresa (plan FREE) y lo asigna como OWNER")
    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request);

    @Operation(summary = "Login de usuario local", description = "Autentica un usuario con email y contraseña")
    @PostMapping("/login")
    ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request);

    @Operation(summary = "Obtener usuario actual", description = "Retorna los datos del usuario autenticado")
    @GetMapping("/user")
    ResponseEntity<ApiResponse<UserEntityResponse>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Obtener tenants del usuario", description = "Lista todas las empresas/tenants a las que pertenece el usuario")
    @GetMapping("/tenants")
    ResponseEntity<ApiResponse<Page<UserTenantResponse>>> getUserTenants(
            Pageable pageable,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Seleccionar tenant activo", description = "Cambia el tenant activo para el usuario")
    @PostMapping("/tenants/select")
    ResponseEntity<ApiResponse<AuthResponse>> selectTenant(
            @Valid @RequestBody SelectTenantRequest request,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Crear nuevo tenant", description = "Crea una nueva empresa/tenant para el usuario autenticado")
    @PostMapping("/tenants")
    ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            @Valid @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Logout", description = "Invalida los tokens y cierra la sesión del usuario")
    @PostMapping("/logout")
    ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request);

    @Operation(summary = "Refresh token", description = "Obtiene un nuevo access token usando el refresh token")
    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request);

    @Operation(summary = "Obtener invitaciones pendientes", description = "Lista las invitaciones pendientes del usuario")
    @GetMapping("/invitations")
    ResponseEntity<ApiResponse<Page<InvitationResponse>>> getPendingInvitations(
            Pageable pageable,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Crear invitación", description = "Invita a un usuario a un tenant")
    @PostMapping("/invitations")
    ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Aceptar invitación", description = "Acepta una invitación usando el token")
    @PostMapping("/invitations/accept")
    ResponseEntity<ApiResponse<InvitationResponse>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Cancelar invitación", description = "Cancela una invitación pendiente")
    @DeleteMapping("/invitations/{invitationId}")
    ResponseEntity<ApiResponse<Void>> cancelInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal OAuth2User principal);

    // ==================== USER MANAGEMENT ====================

    @Operation(summary = "Listar usuarios de un tenant", description = "Lista los miembros activos de un tenant (requiere OWNER o ADMIN)")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')")
    @GetMapping("/tenants/{tenantId}/users")
    ResponseEntity<ApiResponse<Page<UserTenantResponse>>> getTenantUsers(
            @PathVariable UUID tenantId,
            Pageable pageable,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Cambiar rol de usuario", description = "Modifica el rol de un usuario en un tenant (requiere OWNER o ADMIN, validación de jerarquía)")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')")
    @PutMapping("/tenants/{tenantId}/users/{userId}/role")
    ResponseEntity<ApiResponse<UserTenantResponse>> updateUserRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestParam String role,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Desvincular usuario", description = "Elimina un usuario de un tenant (soft delete, solo OWNER)")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    @DeleteMapping("/tenants/{tenantId}/users/{userId}")
    ResponseEntity<ApiResponse<Void>> deleteUserFromTenant(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal OAuth2User principal);
}
