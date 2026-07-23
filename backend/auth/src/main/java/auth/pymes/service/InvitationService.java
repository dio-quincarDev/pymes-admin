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
    /**
     * Obtiene las invitaciones pendientes del usuario (paginado).
     */
    Page<InvitationResponse> getPendingInvitations(Pageable pageable, Object principal);

    /**
     * Crea una invitación para un usuario.
     */
    InvitationResponse createInvitation(CreateInvitationRequest request, Object principal);

    /**
     * Acepta una invitación usando el token.
     */
    InvitationResponse acceptInvitation(String invitationToken, Object principal);

    /**
     * Cancela una invitación.
     */
    void cancelInvitation(UUID invitationId, Object principal);

    /**
     * Obtiene info pública de una invitación por token.
     */
    InvitationInfoResponse getInvitationInfo(String token);

    /**
     * Registra un nuevo usuario y acepta la invitación en un solo paso.
     */
    AuthResponse registerAndAccept(String token, InvitationRegisterRequest request);
}
