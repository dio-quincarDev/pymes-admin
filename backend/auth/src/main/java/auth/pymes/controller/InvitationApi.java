package auth.pymes.controller;

import auth.pymes.common.constants.ApiPathConstants;
import auth.pymes.common.models.dto.request.AcceptInvitationRequest;
import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.response.ApiResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Invitations", description = "Endpoints de gestión de invitaciones")
@RequestMapping(ApiPathConstants.V1_ROUTE + ApiPathConstants.INVITATIONS_ROUTE)
public interface InvitationApi {

    @Operation(summary = "Obtener invitaciones pendientes", description = "Lista las invitaciones pendientes del usuario")
    @GetMapping
    ResponseEntity<ApiResponse<Page<InvitationResponse>>> getPendingInvitations(
            Pageable pageable,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Crear invitación", description = "Invita a un usuario a un tenant")
    @PostMapping
    ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Aceptar invitación", description = "Acepta una invitación usando el token")
    @PostMapping(ApiPathConstants.INVITATIONS_ACCEPT)
    ResponseEntity<ApiResponse<InvitationResponse>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request,
            @AuthenticationPrincipal OAuth2User principal);

    @Operation(summary = "Cancelar invitación", description = "Cancela una invitación pendiente")
    @DeleteMapping("/{invitationId}")
    ResponseEntity<ApiResponse<Void>> cancelInvitation(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal OAuth2User principal);
}
