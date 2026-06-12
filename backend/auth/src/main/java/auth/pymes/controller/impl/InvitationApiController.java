package auth.pymes.controller.impl;

import auth.pymes.common.models.dto.request.AcceptInvitationRequest;
import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.controller.InvitationApi;
import auth.pymes.service.InvitationService;
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
public class InvitationApiController implements InvitationApi {

    private final InvitationService invitationService;

    @Override
    public ResponseEntity<ApiResponse<Page<InvitationResponse>>> getPendingInvitations(
            Pageable pageable, Object principal) {
        Page<InvitationResponse> invitations = invitationService.getPendingInvitations(pageable, principal);
        return ResponseEntity.ok(ApiResponse.ok(invitations));
    }

    @Override
    public ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            CreateInvitationRequest request, Object principal) {
        InvitationResponse response = invitationService.createInvitation(request, principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<InvitationResponse>> acceptInvitation(
            AcceptInvitationRequest request, Object principal) {
        InvitationResponse response = invitationService.acceptInvitation(request.invitationToken(), principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(
            UUID invitationId, Object principal) {
        invitationService.cancelInvitation(invitationId, principal);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
