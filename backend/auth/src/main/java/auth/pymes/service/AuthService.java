package auth.pymes.service;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import auth.pymes.common.models.entities.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de autenticación y gestión de usuarios.
 */
public interface AuthService {

    // ==================== LOCAL AUTH ====================

    /**
     * Registro atómico: crea usuario, tenant (plan FREE) y vínculo OWNER.
     */
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    /**
     * Login con email y contraseña.
     */
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    // ==================== USER MANAGEMENT ====================

    /**
     * Lista los usuarios de un tenant (requiere OWNER o ADMIN).
     */
    Page<UserTenantResponse> getTenantUsers(UUID tenantId, Pageable pageable, OAuth2User principal);

    /**
     * Cambia el rol de un usuario en un tenant (validación de jerarquía).
     */
    UserTenantResponse updateUserRole(UUID tenantId, UUID userId, String newRole, OAuth2User principal);

    /**
     * Desvincula un usuario de un tenant (soft delete con validación de jerarquía).
     */
    void deleteUserFromTenant(UUID tenantId, UUID userId, OAuth2User principal);

    // ==================== USER ====================

    /**
     * Obtiene los datos del usuario autenticado.
     */
    UserEntityResponse getCurrentUser(OAuth2User principal);

    /**
     * Obtiene el usuario por email.
     */
    UserEntityResponse getUserByEmail(String email);

    // ==================== TENANTS ====================

    /**
     * Obtiene todos los tenants del usuario (paginado).
     */
    Page<UserTenantResponse> getUserTenants(Pageable pageable, OAuth2User principal);

    /**
     * Selecciona un tenant activo y genera nuevos tokens.
     */
    AuthResponse selectTenant(SelectTenantRequest request, OAuth2User principal);

    /**
     * Crea un nuevo tenant para el usuario autenticado.
     */
    TenantResponse createTenant(CreateTenantRequest request, OAuth2User principal);

    // ==================== TOKENS ====================

    /**
     * Hace logout del usuario (invalida tokens).
     */
    LogoutResponse logout(String accessToken);

    /**
     * Refresca el access token usando el refresh token.
     */
    AuthResponse refreshToken(TokenRefreshRequest request);

    // ==================== INVITATIONS ====================

    /**
     * Obtiene las invitaciones pendientes del usuario (paginado).
     */
    Page<InvitationResponse> getPendingInvitations(Pageable pageable, OAuth2User principal);

    /**
     * Crea una invitación para un usuario.
     */
    InvitationResponse createInvitation(CreateInvitationRequest request, OAuth2User principal);

    /**
     * Acepta una invitación usando el token.
     */
    InvitationResponse acceptInvitation(String invitationToken, OAuth2User principal);

    /**
     * Cancela una invitación.
     */
    void cancelInvitation(UUID invitationId, OAuth2User principal);
}
