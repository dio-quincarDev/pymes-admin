package auth.pymes.service;

import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

public interface TenantService {
    /**
     * Obtiene todos los tenants del usuario (paginado).
     */
    Page<UserTenantResponse> getUserTenants(Pageable pageable, Authentication authentication);

    /**
     * Selecciona un tenant activo y genera nuevos tokens.
     */
    AuthResponse selectTenant(SelectTenantRequest request, Authentication authentication);

    /**
     * Crea un nuevo tenant para el usuario autenticado.
     */
    TenantResponse createTenant(CreateTenantRequest request, Authentication authentication);
}
