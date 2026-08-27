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
import auth.pymes.repositories.RefreshTokenRepository;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.AuthService;
import auth.pymes.service.EmailVerificationService;
import auth.pymes.service.JwtService;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RateLimitService rateLimitService;
    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final EmailVerificationService emailVerificationService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== LOCAL AUTH ====================

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(USER_ALREADY_EXISTS, request.email());
        }

        if (emailVerificationService.existsPendingRegistration(request.email())) {
            throw new DuplicateResourceException(USER_ALREADY_EXISTS, request.email());
        }

        if (tenantRepository.existsBySlug(request.companySlug())) {
            throw new DuplicateResourceException(TENANT_ALREADY_EXISTS, request.companySlug());
        }

        // PENDING REGISTRATION: Store data in Redis and send email
        emailVerificationService.generateAndSendPendingRegistrationEmail(request);
        log.info("Registro pendiente iniciado para: {}. Email de verificación enviado.", request.email());

        // Return empty response (tokens will be generated after verification)
        return new AuthResponse(null, null, null, null);
    }

    @Override
    @Transactional
    public AuthResponse completeRegistration(RegisterRequest request, HttpServletRequest httpRequest) {
        log.info("Completando registro para: {}", request.email());

        UserEntity user = UserEntity.builder()
                .name(request.name())
                .email(request.email().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .providerId(request.email().toLowerCase())
                .isActive(true)
                .emailVerifiedAt(ZonedDateTime.now()) // Ya viene verificado de Redis
                .build();

        user = userRepository.save(user);
        log.info("Usuario local creado: {}", user.getEmail());

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
        jwtService.saveRefreshToken(user, tenant.getId(), refreshToken);

        auditLoginAction(user, tenant.getId(), "REGISTER_COMPLETE", httpRequest);

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

        if (!user.isEmailVerified()) {
            throw new auth.pymes.utils.exception.auth.AuthorizationException(
                    auth.pymes.utils.exception.CodigoError.EMAIL_NOT_VERIFIED);
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
        jwtService.saveRefreshToken(user, activeTenantId, refreshToken);

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
    @Transactional
    public LogoutResponse logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        boolean sessionsRevoked = false;
        if (accessToken != null && !accessToken.isBlank()) {
            UUID userId = jwtService.extractUserId(accessToken);
            try {
                jwtService.revokeToken(accessToken);
                log.info("Access token revocado");
            } catch (Exception e) {
                log.warn("Error revocando access token (Redis puede estar caído): {}", e.getMessage());
            }
            if (userId != null) {
                try {
                    refreshTokenRepository.deleteByUserId(userId);
                    sessionsRevoked = true;
                    log.info("Global Logout - Todas las sesiones del usuario {} revocadas", userId);
                } catch (Exception e) {
                    log.error("Error eliminando refresh tokens del usuario {}: {}", userId, e.getMessage());
                }
            }
        }

        return new LogoutResponse(true, "Logout successful", Instant.now(), sessionsRevoked);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String oldRefreshToken = request.refreshToken();

        // 1. Validar y rotar el token en la base de datos (Detección de reuso ocurre aquí)
        JwtService.RefreshTokenValidation validation = jwtService.validateAndRevokeRefreshToken(oldRefreshToken);

        // 2. Cargar usuario
        UserEntity user = userRepository.findById(validation.userId())
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_ID, validation.userId()));

        if (!user.isEnabled()) {
            throw new AuthorizationException(USER_INACTIVE);
        }

        // 3. Determinar Tenant Activo y Rol
        UUID activeTenantId = validation.tenantId();
        UserTenant userTenant = null;

        if (activeTenantId != null) {
            userTenant = userTenantRepository.findByUserIdAndTenantId(user.getId(), activeTenantId)
                    .orElse(null);
        }

        // Si el tenant del token no es válido o no existe, buscar el primero disponible
        if (userTenant == null) {
            List<UserTenant> userTenants = userTenantRepository.findByUserIdAndIsActiveTrue(user.getId());
            if (!userTenants.isEmpty()) {
                userTenant = userTenants.get(0);
                activeTenantId = userTenant.getTenantId();
            } else {
                activeTenantId = null;
            }
        }

        String role = userTenant != null ? userTenant.getRole().name() : "VIEWER";
        Tenant activeTenant = activeTenantId != null ? tenantRepository.findById(activeTenantId).orElse(null) : null;
        String plan = activeTenant != null ? activeTenant.getPlan().name() : "FREE";

        // 4. Generar nuevos tokens
        String newAccessToken = jwtService.generateAccessToken(user, activeTenantId, role, plan);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // 5. Persistir el nuevo Refresh Token
        jwtService.saveRefreshToken(user, activeTenantId, newRefreshToken);

        log.info("Refresh token rotado exitosamente para user={}", user.getEmail());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                userMapper.toResponse(user),
                activeTenant != null ? tenantMapper.toResponse(activeTenant) : null
        );
    }

    @Override
    public AuthResponse exchange(String code) {
        @SuppressWarnings("unchecked")
        Map<String, String> tokenData = (Map<String, String>) redisTemplate.opsForValue().get("oauth:code:" + code);
        if (tokenData == null) {
            throw new InvalidInputException(INVALID_INPUT, "Invalid or expired exchange code");
        }
        redisTemplate.delete("oauth:code:" + code);

        String accessToken = tokenData.get("accessToken");
        UUID userId = jwtService.extractUserId(accessToken);
        UUID tenantId = jwtService.extractTenantId(accessToken);

        UserEntity user = userRepository.findById(userId)
                .orElse(null);

        Tenant tenant = tenantId != null ? tenantRepository.findById(tenantId).orElse(null) : null;

        return new AuthResponse(
                accessToken,
                tokenData.get("refreshToken"),
                user != null ? userMapper.toResponse(user) : null,
                tenant != null ? tenantMapper.toResponse(tenant) : null
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
