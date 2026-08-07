package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import auth.pymes.controller.TenantApi;
import auth.pymes.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TenantApiController implements TenantApi {

    private final TenantService tenantService;

    @Override
    public ResponseEntity<ApiResponse<Page<UserTenantResponse>>> getUserTenants(
            Pageable pageable, Authentication authentication) {
        Page<UserTenantResponse> tenants = tenantService.getUserTenants(pageable, authentication);
        return ResponseEntity.ok(ApiResponse.ok(tenants));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> selectTenant(
            SelectTenantRequest request, Authentication authentication) {
        AuthResponse response = tenantService.selectTenant(request, authentication);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            CreateTenantRequest request, Authentication authentication) {
        TenantResponse tenant = tenantService.createTenant(request, authentication);
        return ResponseEntity.ok(ApiResponse.ok(tenant));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> shutdownTenant(
            UUID tenantId, Authentication authentication) {
        tenantService.shutdown(tenantId, authentication);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
