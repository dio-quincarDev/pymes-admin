package auth.pymes.service.impl;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.entities.Invitation;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.mappers.InvitationMapper;
import auth.pymes.repositories.InvitationRepository;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.EmailService;
import auth.pymes.service.InvitationService;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static auth.pymes.utils.exception.CodigoError.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationServiceImpl implements InvitationService {

    private final UserEntityRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public Page<InvitationResponse> getPendingInvitations(Pageable pageable, Object principal) {
        String email = extractEmail(principal);
        Page<Invitation> invitations = invitationRepository.findByEmailAndAcceptedAtIsNull(email, pageable);
        
        return invitations.map(invitation -> {
            Tenant tenant = tenantRepository.findById(invitation.getTenantId()).orElse(null);
            UserEntity inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
            return invitationMapper.toResponse(invitation, tenant, inviter);
        });
    }

    @Override
    @Transactional
    public InvitationResponse createInvitation(CreateInvitationRequest request, Object principal) {
        String inviterEmail = extractEmail(principal);
        UserEntity inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, inviterEmail));

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException(TENANT_NOT_FOUND, request.tenantId()));

        UserTenant inviterTenant = userTenantRepository.findByUserIdAndTenantId(inviter.getId(), request.tenantId())
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, request.tenantId()));

        if (inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.OWNER && inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.ADMIN) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        if (!inviterTenant.getRole().hasMorePowerThan(request.role())) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS,
                    "Cannot invite a user with role equal or higher than yours");
        }

        long currentMembers = userTenantRepository.countByTenantIdAndIsActiveTrue(request.tenantId());
        if (currentMembers >= tenant.getMaxUsers()) {
            throw new DuplicateResourceException(MAX_USERS_REACHED, tenant.getMaxUsers());
        }

        if (invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(request.tenantId(), request.email())) {
            throw new DuplicateResourceException(EMAIL_ALREADY_INVITED, request.email());
        }

        UserEntity existingUser = userRepository.findByEmail(request.email()).orElse(null);
        if (existingUser != null) {
            if (userTenantRepository.findByUserIdAndTenantId(existingUser.getId(), request.tenantId()).isPresent()) {
                throw new DuplicateResourceException(DUPLICATE_RESOURCE, "User is already a member of this tenant");
            }
        }

        String token = UUID.randomUUID().toString();
        Invitation invitation = Invitation.builder()
                .tenantId(request.tenantId())
                .email(request.email())
                .role(request.role())
                .invitedBy(inviter.getId())
                .token(token)
                .expiresAt(ZonedDateTime.now().plusDays(7))
                .build();

        invitation = invitationRepository.save(invitation);
        log.info("Usuario {} invitó a {} al tenant {} con rol {}", 
                inviter.getEmail(), request.email(), tenant.getName(), request.role());

        sendInvitationEmail(inviter, tenant, invitation);

        return invitationMapper.toResponse(invitation, tenant, inviter);
    }

    @Override
    @Transactional
    public InvitationResponse acceptInvitation(String invitationToken, Object principal) {
        String email = extractEmail(principal);

        Invitation invitation = invitationRepository.findByTokenAndAcceptedAtIsNull(invitationToken)
                .orElseThrow(() -> new ResourceNotFoundException(INVITATION_NOT_FOUND, invitationToken));

        if (!invitation.getEmail().equals(email)) {
            throw new AuthorizationException(INVALID_INPUT, "Invitation email does not match authenticated user");
        }

        if (invitation.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new InvalidInputException(INVITATION_EXPIRED);
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(invitation.getTenantId())
                .role(invitation.getRole())
                .invitedBy(invitation.getInvitedBy())
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();

        userTenantRepository.save(userTenant);

        invitation.setAcceptedAt(ZonedDateTime.now());
        invitationRepository.save(invitation);

        log.info("Usuario {} aceptó invitación al tenant {}", user.getEmail(), invitation.getTenantId());

        Tenant tenant = tenantRepository.findById(invitation.getTenantId()).orElse(null);
        UserEntity inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);

        return invitationMapper.toResponse(invitation, tenant, inviter);
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID invitationId, Object principal) {
        String inviterEmail = extractEmail(principal);
        UserEntity inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, inviterEmail));

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException(INVITATION_NOT_FOUND, invitationId));

        if (!invitation.getInvitedBy().equals(inviter.getId())) {
            UserTenant inviterTenant = userTenantRepository.findByUserIdAndTenantId(inviter.getId(), invitation.getTenantId())
                    .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT));
            
            if (inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.OWNER && inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.ADMIN) {
                throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
            }
        }

        invitationRepository.delete(invitation);
        log.info("Invitación {} cancelada por {}", invitationId, inviter.getEmail());
    }

    private String extractEmail(Object principal) {
        if (principal instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String email) {
            return email;
        }
        throw new AuthorizationException(UNAUTHORIZED_ACCESS, "Could not extract email from principal");
    }

    private void sendInvitationEmail(UserEntity inviter, Tenant tenant, Invitation invitation) {
        String baseUrl = frontendUrl.replace(":9000", ":9200");
        String acceptUrl = baseUrl + "/#/accept-invitation?token=" + invitation.getToken();

        Map<String, Object> variables = Map.of(
                "inviterName", inviter.getName(),
                "tenantName", tenant.getName(),
                "url", acceptUrl
        );

        emailService.send(invitation.getEmail(),
                "Fuiste invitado a unirte a " + tenant.getName() + " en Pymes Admin",
                "invitation",
                variables);
    }

}
