package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.request.AcceptInvitationRequest;
import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import auth.pymes.controller.AuthApi;
import auth.pymes.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthApiController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<ApiResponse<UserEntityResponse>> getCurrentUser(OAuth2User principal) {
        UserEntityResponse user = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @Override
    public ResponseEntity<ApiResponse<Page<UserTenantResponse>>> getUserTenants(
            Pageable pageable, OAuth2User principal) {
        Page<UserTenantResponse> tenants = authService.getUserTenants(pageable, principal);
        return ResponseEntity.ok(ApiResponse.ok(tenants));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> selectTenant(
            SelectTenantRequest request, OAuth2User principal) {
        AuthResponse response = authService.selectTenant(request, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
            CreateTenantRequest request, OAuth2User principal) {
        TenantResponse tenant = authService.createTenant(request, principal);
        return ResponseEntity.ok(ApiResponse.ok(tenant));
    }

    @Override
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(HttpServletRequest request) {
        // Extraer el token del header Authorization
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        
        LogoutResponse response = authService.logout(accessToken);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(TokenRefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Page<InvitationResponse>>> getPendingInvitations(
            Pageable pageable, OAuth2User principal) {
        Page<InvitationResponse> invitations = authService.getPendingInvitations(pageable, principal);
        return ResponseEntity.ok(ApiResponse.ok(invitations));
    }

    @Override
    public ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            CreateInvitationRequest request, OAuth2User principal) {
        InvitationResponse response = authService.createInvitation(request, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<InvitationResponse>> acceptInvitation(
            AcceptInvitationRequest request, OAuth2User principal) {
        InvitationResponse response = authService.acceptInvitation(request.invitationToken(), principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(
            UUID invitationId, OAuth2User principal) {
        authService.cancelInvitation(invitationId, principal);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
