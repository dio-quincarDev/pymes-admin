package auth.pymes.service.impl;

import auth.pymes.common.config.RateLimitService;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.common.models.entities.AuditLog;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.common.models.mappers.TenantMapper;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.AuditLogRepository;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.AuthService;
import auth.pymes.service.JwtService;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static auth.pymes.utils.exception.CodigoError.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserEntityRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RateLimitService rateLimitService;
    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    // ==================== LOCAL AUTH ====================

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(USER_ALREADY_EXISTS, request.email());
        }

        if (tenantRepository.existsBySlug(request.companySlug())) {
            throw new DuplicateResourceException(TENANT_ALREADY_EXISTS, request.companySlug());
        }

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

        Tenant tenant = Tenant.builder()
                .name(request.companyName())
                .slug(request.companySlug())
                .plan(PlanName.FREE)
                .maxUsers(1)
                .isActive(true)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant aprovisionado: {} ({})", tenant.getName(), tenant.getId());

        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .role(RoleName.OWNER)
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();

        userTenantRepository.save(userTenant);

        String accessToken = jwtService.generateAccessToken(user, tenant.getId(), RoleName.OWNER.name(), tenant.getPlan().name());
        String refreshToken = jwtService.generateRefreshToken(user);

        auditLoginAction(user, tenant.getId(), "REGISTER", httpRequest);

        return new AuthResponse(accessToken, refreshToken, userMapper.toResponse(user), tenantMapper.toResponse(tenant));
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String rateLimitKey = "login:" + httpRequest.getRemoteAddr() + ":" + request.email();
        if (!rateLimitService.isAllowed(rateLimitKey)) {
            throw new InvalidInputException(RATE_LIMIT_EXCEEDED, rateLimitService.getRemainingAttempts(rateLimitKey));
        }

        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationException(INVALID_CREDENTIALS));

        if (!user.isEnabled()) {
            throw new AuthorizationException(USER_INACTIVE);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }

        List<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId());
        UUID activeTenantId = userTenants.isEmpty() ? null : userTenants.get(0).getTenantId();
        String role = userTenants.isEmpty() ? "VIEWER" : userTenants.get(0).getRole().name();

        Tenant activeTenant = null;
        if (activeTenantId != null) {
            activeTenant = tenantRepository.findById(activeTenantId).orElse(null);
        }

        String plan = activeTenant != null ? activeTenant.getPlan().name() : "FREE";
        String accessToken = jwtService.generateAccessToken(user, activeTenantId, role, plan);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Usuario {} hizo login exitoso", user.getEmail());

        UUID tenantId = activeTenant != null ? activeTenant.getId() : null;
        auditLoginAction(user, tenantId, "LOGIN", httpRequest);

        return new AuthResponse(
                accessToken,
                refreshToken,
                userMapper.toResponse(user),
                activeTenant != null ? tenantMapper.toResponse(activeTenant) : null
        );
    }

    // ==================== TOKENS ====================

    @Override
    public LogoutResponse logout(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
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

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new TokenExpiredException("Refresh token has expired or is invalid");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_ID, userId));

        if (!user.isEnabled()) {
            throw new AuthorizationException(USER_INACTIVE);
        }

        List<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId());
        UUID activeTenantId = userTenants.isEmpty() ? null : userTenants.get(0).getTenantId();
        String role = userTenants.isEmpty() ? "VIEWER" : userTenants.get(0).getRole().name();

        Tenant activeTenant = null;
        if (activeTenantId != null) {
            activeTenant = tenantRepository.findById(activeTenantId).orElse(null);
        }

        String plan = activeTenant != null ? activeTenant.getPlan().name() : "FREE";
        String newAccessToken = jwtService.generateAccessToken(user, activeTenantId, role, plan);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                userMapper.toResponse(user),
                activeTenant != null ? tenantMapper.toResponse(activeTenant) : null
        );
    }

    // ==================== AUDIT HELPER ====================

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
