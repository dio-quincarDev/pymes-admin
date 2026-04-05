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
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TenantApiController implements TenantApi {

    private final TenantService tenantService;

    @Override
    public ResponseEntity<ApiResponse<Page<UserTenantResponse>>> getUserTenants(
            Pageable pageable, OAuth2User principal) {
        Page<UserTenantResponse> tenants = tenantService.getUserTenants(pageable, principal);
        return ResponseEntity.ok(ApiResponse.ok(tenants));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> selectTenant(
            SelectTenantRequest request, OAuth2User principal) {
        AuthResponse response = tenantService.selectTenant(request, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            CreateTenantRequest request, OAuth2User principal) {
        TenantResponse tenant = tenantService.createTenant(request, principal);
        return ResponseEntity.ok(ApiResponse.ok(tenant));
    }
}
