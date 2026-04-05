package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.MemberResponse;
import auth.pymes.controller.MemberApi;
import auth.pymes.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MemberApiController implements MemberApi {

    private final MemberService memberService;

    @Override
    public ResponseEntity<ApiResponse<Page<MemberResponse>>> getTenantUsers(
            UUID tenantId, Pageable pageable, OAuth2User principal) {
        Page<MemberResponse> users = memberService.getTenantUsers(tenantId, pageable, principal);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @Override
    public ResponseEntity<ApiResponse<MemberResponse>> updateUserRole(
            UUID tenantId, UUID userId, String role, OAuth2User principal) {
        MemberResponse response = memberService.updateUserRole(tenantId, userId, role, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteUserFromTenant(
            UUID tenantId, UUID userId, OAuth2User principal) {
        memberService.deleteUserFromTenant(tenantId, userId, principal);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
