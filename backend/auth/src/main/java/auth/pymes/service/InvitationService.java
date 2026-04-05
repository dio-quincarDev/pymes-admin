package auth.pymes.service;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.response.InvitationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

public interface InvitationService {
    /**
     * Obtiene las invitaciones pendientes del usuario (paginado).
     */
    Page<InvitationResponse> getPendingInvitations(Pageable pageable, OAuth2User principal);

    /**
     * Crea una invitación para un usuario.
     */
    InvitationResponse createInvitation(CreateInvitationRequest request, OAuth2User principal);

    /**
     * Acepta una invitación usando el token.
     */
    InvitationResponse acceptInvitation(String invitationToken, OAuth2User principal);

    /**
     * Cancela una invitación.
     */
    void cancelInvitation(UUID invitationId, OAuth2User principal);
}
