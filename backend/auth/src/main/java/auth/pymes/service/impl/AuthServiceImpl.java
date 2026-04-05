package auth.pymes.service.impl;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import auth.pymes.common.config.RateLimitService;
import auth.pymes.common.models.entities.AuditLog;
import auth.pymes.common.models.entities.Invitation;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.repositories.*;
import auth.pymes.service.AuthService;
import auth.pymes.service.JwtService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import auth.pymes.utils.exception.token.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AuditLogRepository auditLogRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RateLimitService rateLimitService;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    // ==================== LOCAL AUTH ====================

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        // 1. Validar unicidad de email
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(USER_ALREADY_EXISTS, request.email());
        }

        // 2. Validar unicidad de slug del tenant
        if (tenantRepository.existsBySlug(request.companySlug())) {
            throw new DuplicateResourceException(TENANT_ALREADY_EXISTS, request.companySlug());
        }

        // 3. Crear UserEntity (Dueño)
        UserEntity user = UserEntity.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .providerId(request.email())
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("Usuario local registrado: {}", user.getEmail());

        // 4. Crear Tenant (Empresa) con Plan FREE por defecto
        Tenant tenant = Tenant.builder()
                .name(request.companyName())
                .slug(request.companySlug())
                .plan(PlanName.FREE)
                .maxUsers(1)
                .isActive(true)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant aprovisionado: {} ({})", tenant.getName(), tenant.getId());

        // 5. Vincular UserTenant con rol OWNER
        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .role(RoleName.OWNER)
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();

        userTenantRepository.save(userTenant);

        // 6. Generar tokens
        String accessToken = jwtService.generateAccessToken(user, tenant.getId(), RoleName.OWNER.name(), tenant.getPlan().name());
        String refreshToken = jwtService.generateRefreshToken(user);

        // 7. Auditoría forense (IA Ready)
        auditLoginAction(user, tenant.getId(), "REGISTER", httpRequest);

        return new AuthResponse(accessToken, refreshToken, mapToUserResponse(user), mapToTenantResponse(tenant));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // 0. Rate limiting
        String rateLimitKey = "login:" + httpRequest.getRemoteAddr();
        if (!rateLimitService.isAllowed(rateLimitKey)) {
            throw new InvalidInputException(RATE_LIMIT_EXCEEDED, rateLimitService.getRemainingAttempts(rateLimitKey));
        }

        // 1. Buscar usuario por email
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

        // 2. Verificar que el usuario está habilitado
        if (!user.isEnabled()) {
            throw new AuthorizationException(USER_INACTIVE);
        }

        // 3. Autenticar credenciales
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        // 4. Obtener tenants del usuario
        List<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId());
        UUID activeTenantId = userTenants.isEmpty() ? null : userTenants.get(0).getTenantId();
        String role = userTenants.isEmpty() ? "VIEWER" : userTenants.get(0).getRole().name();

        Tenant activeTenant = null;
        if (activeTenantId != null) {
            activeTenant = tenantRepository.findById(activeTenantId).orElse(null);
        }

        // 5. Generar tokens
        String plan = activeTenant != null ? activeTenant.getPlan().name() : "FREE";
        String accessToken = jwtService.generateAccessToken(user, activeTenantId, role, plan);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Usuario {} hizo login exitoso", user.getEmail());

        // Auditoría forense (IA Ready)
        UUID tenantId = activeTenant != null ? activeTenant.getId() : null;
        auditLoginAction(user, tenantId, "LOGIN", httpRequest);

        return new AuthResponse(
                accessToken,
                refreshToken,
                mapToUserResponse(user),
                activeTenant != null ? mapToTenantResponse(activeTenant) : null
        );
    }

    // ==================== USER MANAGEMENT ====================

    @Override
    public Page<UserTenantResponse> getTenantUsers(UUID tenantId, Pageable pageable, OAuth2User principal) {
        String requesterEmail = principal.getAttribute("email");
        UserEntity requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, requesterEmail));

        // Verificar que el requester pertenece al tenant y es OWNER/ADMIN
        UserTenant requesterRelation = userTenantRepository.findByUserIdAndTenantId(requester.getId(), tenantId)
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, tenantId));

        if (requesterRelation.getRole() != RoleName.OWNER && requesterRelation.getRole() != RoleName.ADMIN) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(TENANT_NOT_FOUND, tenantId));

        // Contar miembros activos para paginación manual (PageImpl necesita lista + total)
        // Usamos findByUserIdAndIsActiveTrue iterado por tenant via UserTenant
        Page<UserTenant> userTenants = userTenantRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable);

        return userTenants.map(ut -> UserTenantResponse.forMember(
                ut.getUserId(),
                ut.getUser().getName(),
                ut.getUser().getEmail(),
                ut.getRole(),
                ut.getAcceptedAt() != null,
                ut.getCreatedAt()
        ));
    }

    @Override
    @Transactional
    public UserTenantResponse updateUserRole(UUID tenantId, UUID userId, String newRole, OAuth2User principal) {
        String requesterEmail = principal.getAttribute("email");
        UserEntity requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, requesterEmail));

        // Verificar que el requester pertenece al tenant
        UserTenant requesterRelation = userTenantRepository.findByUserIdAndTenantId(requester.getId(), tenantId)
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, tenantId));

        // Verificar permisos (OWNER o ADMIN)
        if (requesterRelation.getRole() != RoleName.OWNER && requesterRelation.getRole() != RoleName.ADMIN) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        // Obtener la relación del usuario objetivo
        UserTenant targetRelation = userTenantRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_IN_TENANT, userId));

        RoleName targetRole = targetRelation.getRole();
        RoleName newRoleEnum;
        try {
            newRoleEnum = RoleName.valueOf(newRole);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException(INVALID_ROLE, newRole);
        }

        // Jerarquía: no puedes modificar a alguien de rol igual o mayor
        if (!requesterRelation.getRole().hasMorePowerThan(targetRole)) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS,
                    "Cannot modify a user with role equal or higher than yours");
        }

        // SaaS Integrity: OWNER no puede ser degradado
        if (targetRole == RoleName.OWNER) {
            throw new AuthorizationException(OWNER_CANNOT_BE_REMOVED);
        }

        targetRelation.setRole(newRoleEnum);
        userTenantRepository.save(targetRelation);

        log.info("Usuario {} cambió rol de userId={} a {} en tenant {}",
                requester.getEmail(), userId, newRole, tenantId);

        return UserTenantResponse.forMember(
                targetRelation.getUserId(),
                targetRelation.getUser().getName(),
                targetRelation.getUser().getEmail(),
                newRoleEnum,
                targetRelation.getAcceptedAt() != null,
                targetRelation.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteUserFromTenant(UUID tenantId, UUID userId, OAuth2User principal) {
        String requesterEmail = principal.getAttribute("email");
        UserEntity requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, requesterEmail));

        // Verificar que el requester pertenece al tenant
        UserTenant requesterRelation = userTenantRepository.findByUserIdAndTenantId(requester.getId(), tenantId)
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, tenantId));

        // Solo OWNER puede desvincular usuarios
        if (requesterRelation.getRole() != RoleName.OWNER) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        UserTenant targetRelation = userTenantRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_IN_TENANT, userId));

        // Jerarquía: OWNER no puede desvincular a otro OWNER
        if (targetRelation.getRole() == RoleName.OWNER) {
            throw new AuthorizationException(OWNER_CANNOT_BE_REMOVED);
        }

        // SaaS Integrity: OWNER no puede desvincularse a sí mismo
        if (requester.getId().equals(userId)) {
            throw new AuthorizationException(OWNER_CANNOT_BE_REMOVED,
                    "Owner cannot remove themselves. Transfer ownership first.");
        }

        // Soft delete
        userTenantRepository.delete(targetRelation);

        log.info("Usuario {} desvinculó userId={} del tenant {}", requester.getEmail(), userId, tenantId);
    }

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
        String accessToken = jwtService.generateAccessToken(user, tenant.getId(), userTenant.getRole().name(), tenant.getPlan().name());
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

        Tenant activeTenant = null;
        if (activeTenantId != null) {
            activeTenant = tenantRepository.findById(activeTenantId).orElse(null);
        }

        // Generar nuevos tokens
        String plan = activeTenant != null ? activeTenant.getPlan().name() : "FREE";
        String newAccessToken = jwtService.generateAccessToken(user, activeTenantId, role, plan);
        String newRefreshToken = jwtService.generateRefreshToken(user);

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

        // Validación de jerarquía: no se puede invitar a alguien con rol igual o mayor
        if (!inviterTenant.getRole().hasMorePowerThan(request.role())) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS,
                    "Cannot invite a user with role equal or higher than yours");
        }

        // Validación de límites de plan
        long currentMembers = userTenantRepository.countByTenantIdAndIsActiveTrue(request.tenantId());
        if (currentMembers >= tenant.getMaxUsers()) {
            throw new auth.pymes.utils.exception.custom.DuplicateResourceException(
                    CodigoError.MAX_USERS_REACHED, tenant.getMaxUsers());
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

    // ==================== AUDIT HELPER (IA Ready) ====================

    private void auditLoginAction(UserEntity user, UUID tenantId, String action, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuditLog audit = AuditLog.builder()
                .tenantId(tenantId)
                .userId(user.getId())
                .action(action)
                .resource("AUTH")
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(ZonedDateTime.now())
                .build();

        auditLogRepository.save(audit);
        log.info("Auditoría {}: user={} ip={}", action, user.getEmail(), ipAddress);
    }

    private String extractIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
