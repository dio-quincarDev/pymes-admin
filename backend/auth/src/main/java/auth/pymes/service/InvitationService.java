package auth.pymes.service;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.InvitationRegisterRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.InvitationInfoResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InvitationService {

    InvitationInfoResponse getInvitationInfo(String token);

    AuthResponse registerAndAccept(String token, InvitationRegisterRequest request);

    Page<InvitationResponse> getPendingInvitations(Pageable pageable, Object principal);

    InvitationResponse createInvitation(CreateInvitationRequest request, Object principal);

    InvitationResponse acceptInvitation(String invitationToken, Object principal);

    void cancelInvitation(UUID invitationId, Object principal);
}
