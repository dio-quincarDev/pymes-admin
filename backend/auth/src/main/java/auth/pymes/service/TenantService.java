package auth.pymes.service;

import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface TenantService {
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
}
