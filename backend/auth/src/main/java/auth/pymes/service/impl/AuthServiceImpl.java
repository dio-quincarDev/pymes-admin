package auth.pymes.service.impl;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import auth.pymes.common.models.entities.Invitation;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.repositories.InvitationRepository;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.AuthService;
import auth.pymes.service.JwtService;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import auth.pymes.utils.exception.token.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static auth.pymes.utils.exception.CodigoError.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserEntityRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final InvitationRepository invitationRepository;
    private final JwtService jwtService;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    // ==================== USER ====================

    @Override
    public UserEntityResponse getCurrentUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return getUserByEmail(email);
    }

    @Override
    public UserEntityResponse getUserByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));
        
        return mapToUserResponse(user);
    }

    // ==================== TENANTS ====================

    @Override
    public Page<UserTenantResponse> getUserTenants(Pageable pageable, OAuth2User principal) {
        String email = principal.getAttribute("email");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        Page<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId(), pageable);

        return userTenants.map(ut -> new UserTenantResponse(
                ut.getTenantId(),
                ut.getTenant().getName(),
                ut.getTenant().getSlug(),
                ut.getRole(),
                ut.getAcceptedAt() != null,
                ut.getAcceptedAt()
        ));
    }

    @Override
    @Transactional
    public AuthResponse selectTenant(SelectTenantRequest request, OAuth2User principal) {
        String email = principal.getAttribute("email");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        // Verificar que el usuario pertenece al tenant
        UserTenant userTenant = userTenantRepository.findByUserIdAndTenantId(user.getId(), request.tenantId())
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, request.tenantId()));

        if (!userTenant.getIsActive()) {
            throw new AuthorizationException(USER_NOT_IN_TENANT, "User access to tenant is inactive");
        }

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException(TENANT_NOT_FOUND, request.tenantId()));

        if (!tenant.getIsActive()) {
            throw new AuthorizationException(TENANT_INACTIVE);
        }

        // Generar nuevos tokens con el tenant seleccionado
        String accessToken = jwtService.generateAccessToken(user, tenant.getId(), userTenant.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Usuario {} seleccionó tenant {} ({})", user.getEmail(), tenant.getName(), tenant.getId());

        return new AuthResponse(accessToken, refreshToken, mapToUserResponse(user), mapToTenantResponse(tenant));
    }

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request, OAuth2User principal) {
        String email = principal.getAttribute("email");
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        // Verificar si el slug ya existe
        if (tenantRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException(TENANT_ALREADY_EXISTS, request.slug());
        }

        // Crear el tenant
        Tenant tenant = Tenant.builder()
                .name(request.name())
                .slug(request.slug())
                .industry(request.industry())
                .plan(auth.pymes.common.models.enums.PlanName.FREE)
                .maxUsers(1)
                .isActive(true)
                .build();

        tenant = tenantRepository.save(tenant);

        // Crear la relación user-tenant con rol OWNER
        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .role(RoleName.OWNER)
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();

        userTenantRepository.save(userTenant);

        log.info("Usuario {} creó tenant {} ({})", user.getEmail(), tenant.getName(), tenant.getId());

        return mapToTenantResponse(tenant);
    }

    // ==================== TOKENS ====================

    @Override
    public LogoutResponse logout(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                // Revocar el token (lo añade a la blacklist en Redis)
                jwtService.revokeToken(accessToken);
                log.info("Logout exitoso - token revocado");
            } catch (Exception e) {
                log.warn("Error al revocar token: {}", e.getMessage());
            }
        }

        return new LogoutResponse(true, "Logout successful", Instant.now());
    }

    @Override
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();

        // Validar el refresh token
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new TokenExpiredException("Refresh token has expired or is invalid");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        String email = jwtService.extractEmail(refreshToken);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_ID, userId));

        if (!user.isEnabled()) {
            throw new AuthorizationException(USER_INACTIVE);
        }

        // Obtener el tenant actual del usuario (si tiene)
        List<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId());
        UUID activeTenantId = userTenants.isEmpty() ? null : userTenants.get(0).getTenantId();
        String role = userTenants.isEmpty() ? "VIEWER" : userTenants.get(0).getRole().name();

        // Generar nuevos tokens
        String newAccessToken = jwtService.generateAccessToken(user, activeTenantId, role);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        Tenant activeTenant = null;
        if (activeTenantId != null) {
            activeTenant = tenantRepository.findById(activeTenantId).orElse(null);
        }

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                mapToUserResponse(user),
                activeTenant != null ? mapToTenantResponse(activeTenant) : null
        );
    }

    // ==================== INVITATIONS ====================

    @Override
    public Page<InvitationResponse> getPendingInvitations(Pageable pageable, OAuth2User principal) {
        String email = principal.getAttribute("email");

        Page<Invitation> invitations = invitationRepository.findByEmailAndAcceptedAtIsNull(email, pageable);

        return invitations.map(this::mapToInvitationResponse);
    }

    @Override
    @Transactional
    public InvitationResponse createInvitation(CreateInvitationRequest request, OAuth2User principal) {
        String inviterEmail = principal.getAttribute("email");
        UserEntity inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, inviterEmail));

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException(TENANT_NOT_FOUND, request.tenantId()));

        // Verificar que el inviter es OWNER o ADMIN del tenant
        UserTenant inviterTenant = userTenantRepository.findByUserIdAndTenantId(inviter.getId(), request.tenantId())
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, request.tenantId()));

        if (inviterTenant.getRole() != RoleName.OWNER && inviterTenant.getRole() != RoleName.ADMIN) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        // Verificar si el email ya está invitado
        if (invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(request.tenantId(), request.email())) {
            throw new DuplicateResourceException(EMAIL_ALREADY_INVITED, request.email());
        }

        // Verificar si el usuario ya es miembro
        UserEntity existingUser = userRepository.findByEmail(request.email()).orElse(null);
        if (existingUser != null) {
            if (userTenantRepository.findByUserIdAndTenantId(existingUser.getId(), request.tenantId()).isPresent()) {
                throw new DuplicateResourceException(DUPLICATE_RESOURCE, "User is already a member of this tenant");
            }
        }

        // Crear invitación
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

        return mapToInvitationResponse(invitation);
    }

    @Override
    @Transactional
    public InvitationResponse acceptInvitation(String invitationToken, OAuth2User principal) {
        String email = principal.getAttribute("email");

        Invitation invitation = invitationRepository.findByTokenAndAcceptedAtIsNull(invitationToken)
                .orElseThrow(() -> new ResourceNotFoundException(INVITATION_NOT_FOUND, invitationToken));

        // Verificar que el email coincide
        if (!invitation.getEmail().equals(email)) {
            throw new AuthorizationException(INVALID_INPUT, "Invitation email does not match authenticated user");
        }

        // Verificar que no ha expirado
        if (invitation.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new InvalidInputException(INVITATION_EXPIRED);
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        // Crear la relación user-tenant
        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(invitation.getTenantId())
                .role(invitation.getRole())
                .invitedBy(invitation.getInvitedBy())
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();

        userTenantRepository.save(userTenant);

        // Marcar invitación como aceptada
        invitation.setAcceptedAt(ZonedDateTime.now());
        invitationRepository.save(invitation);

        log.info("Usuario {} aceptó invitación al tenant {}", user.getEmail(), invitation.getTenantId());

        return mapToInvitationResponse(invitation);
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID invitationId, OAuth2User principal) {
        String inviterEmail = principal.getAttribute("email");
        UserEntity inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, inviterEmail));

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException(INVITATION_NOT_FOUND, invitationId));

        // Verificar que el inviter es el mismo que creó la invitación o es OWNER/ADMIN
        if (!invitation.getInvitedBy().equals(inviter.getId())) {
            UserTenant inviterTenant = userTenantRepository.findByUserIdAndTenantId(inviter.getId(), invitation.getTenantId())
                    .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT));
            
            if (inviterTenant.getRole() != RoleName.OWNER && inviterTenant.getRole() != RoleName.ADMIN) {
                throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
            }
        }

        invitationRepository.delete(invitation);

        log.info("Invitación {} cancelada por {}", invitationId, inviter.getEmail());
    }

    // ==================== MAPPERS ====================

    private UserEntityResponse mapToUserResponse(UserEntity user) {
        return new UserEntityResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPictureUrl(),
                user.getProvider()
        );
    }

    private TenantResponse mapToTenantResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getPlan(),
                tenant.getIndustry(),
                tenant.getLogoUrl()
        );
    }

    private InvitationResponse mapToInvitationResponse(Invitation invitation) {
        Tenant tenant = tenantRepository.findById(invitation.getTenantId()).orElse(null);
        UserEntity inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);

        return new InvitationResponse(
                invitation.getId(),
                invitation.getTenantId(),
                tenant != null ? tenant.getName() : null,
                invitation.getEmail(),
                invitation.getRole(),
                inviter != null ? inviter.getName() : null,
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt() != null
        );
    }
}
