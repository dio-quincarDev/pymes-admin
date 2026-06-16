package auth.pymes.service;

import auth.pymes.common.models.dto.response.MemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.UUID;

public interface MemberService {

    Page<MemberResponse> getTenantUsers(UUID tenantId, Pageable pageable, Object principal);

    MemberResponse updateUserRole(UUID tenantId, UUID userId, String newRole, Object principal);

    void deleteUserFromTenant(UUID tenantId, UUID userId, Object principal);
}
