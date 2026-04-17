package auth.pymes.controller;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Members", description = "Endpoints de gestión de miembros dentro de un tenant")
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.TENANTS_ROUTE + "/{tenantId}" + ApiPathConstants.MEMBERS_ROUTE)
public interface MemberApi {

    @Operation(summary = "Listar usuarios de un tenant", description = "Lista los miembros activos de un tenant (requiere OWNER o ADMIN)")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')")
    @GetMapping
    ResponseEntity<ApiResponse<Page<MemberResponse>>> getTenantUsers(
            @PathVariable UUID tenantId,
            Pageable pageable,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Cambiar rol de usuario", description = "Modifica el rol de un usuario en un tenant (requiere OWNER o ADMIN, validación de jerarquía)")
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_ADMIN')")
    @PutMapping("/{userId}/role")
    ResponseEntity<ApiResponse<MemberResponse>> updateUserRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestParam String role,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Desvincular usuario", description = "Elimina un usuario de un tenant (soft delete, solo OWNER)")
    @PreAuthorize("hasAuthority('ROLE_OWNER')")
    @DeleteMapping("/{userId}")
    ResponseEntity<ApiResponse<Void>> deleteUserFromTenant(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal OAuth2User principal);
}
