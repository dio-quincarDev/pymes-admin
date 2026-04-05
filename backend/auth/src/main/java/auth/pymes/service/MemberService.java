package auth.pymes.service;

import auth.pymes.common.models.dto.response.MemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

public interface MemberService {
    /**
     * Lista los usuarios de un tenant (requiere OWNER o ADMIN).
     */
    Page<MemberResponse> getTenantUsers(UUID tenantId, Pageable pageable, OAuth2User principal);

    /**
     * Cambia el rol de un usuario en un tenant (validación de jerarquía).
     */
    MemberResponse updateUserRole(UUID tenantId, UUID userId, String newRole, OAuth2User principal);

    /**
     * Desvincula un usuario de un tenant (soft delete con validación de jerarquía).
     */
    void deleteUserFromTenant(UUID tenantId, UUID userId, OAuth2User principal);
}
