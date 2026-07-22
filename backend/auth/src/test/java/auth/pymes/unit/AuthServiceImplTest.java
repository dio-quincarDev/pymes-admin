package auth.pymes.unit;

import auth.pymes.common.config.RateLimitService;
import auth.pymes.common.models.dto.request.LoginRequest;
import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.LogoutResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
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
import auth.pymes.service.EmailVerificationService;
import auth.pymes.service.JwtService;
import auth.pymes.service.impl.AuthServiceImpl;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.token.TokenExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserTenantRepository userTenantRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_WithValidRequest_ReturnsAuthResponseWithNullTokens() {
        RegisterRequest request = new RegisterRequest(
                "New User", "new@example.com", "password", "New Company", "new-company", null
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        doNothing().when(emailVerificationService).generateAndSendPendingRegistrationEmail(any());

        AuthResponse response = authService.register(request, httpRequest);

        assertThat(response.accessToken()).isNull();
        assertThat(response.refreshToken()).isNull();
        verify(emailVerificationService).generateAndSendPendingRegistrationEmail(eq(request));
    }

    @Test
    void completeRegistration_WithValidRequest_ReturnsAuthResponse() {
        RegisterRequest request = new RegisterRequest(
                "New User", "new@example.com", "password", "New Company", "new-company", null
        );

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .name(request.name())
                .build();
        
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name(request.companyName())
                .slug(request.companySlug())
                .plan(PlanName.FREE)
                .build();

        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any())).thenReturn(user);
        when(tenantRepository.save(any())).thenReturn(tenant);
        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(user.getId(), user.getEmail(), user.getName(), null, AuthProvider.LOCAL, null, null, null));
        when(tenantMapper.toResponse(any())).thenReturn(new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSlug(), PlanName.FREE, null, null));
        
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any())).thenReturn(refreshToken);

        AuthResponse response = authService.completeRegistration(request, httpRequest);

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        assertThat(response.user().email()).isEqualTo(request.email());
        verify(jwtService).saveRefreshToken(eq(user), eq(tenant.getId()), eq(refreshToken));
    }

    @Test
    void login_WithValidCredentials_ReturnsAuthResponse() {
        LoginRequest request = new LoginRequest("user@example.com", "password");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isAllowed(anyString())).thenReturn(true);

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .isActive(true)
                .emailVerifiedAt(ZonedDateTime.now())
                .build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any())).thenReturn(null);

        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Test Tenant")
                .plan(PlanName.FREE)
                .build();
        UserTenant userTenant = UserTenant.builder()
                .tenantId(tenant.getId())
                .role(RoleName.OWNER)
                .build();
        when(userTenantRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of(userTenant));
        when(tenantRepository.findById(tenant.getId())).thenReturn(Optional.of(tenant));

        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(user.getId(), user.getEmail(), "Name", null, AuthProvider.LOCAL, null, null, null));
        when(tenantMapper.toResponse(any())).thenReturn(new TenantResponse(tenant.getId(), tenant.getName(), "slug", PlanName.FREE, "TECH", null));

        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        AuthResponse response = authService.login(request, httpRequest);

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.user().email()).isEqualTo(request.email());
        verify(jwtService).saveRefreshToken(user, tenant.getId(), refreshToken);
    }

    @Test
    void logout_WithValidToken_ReturnsLogoutResponseAndRevokesAllSessions() {
        String accessToken = "valid-access-token";
        UUID userId = UUID.randomUUID();

        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + accessToken);
        when(jwtService.extractUserId(accessToken)).thenReturn(userId);
        doNothing().when(jwtService).revokeToken(accessToken);
        doNothing().when(refreshTokenRepository).deleteByUserId(userId);

        LogoutResponse response = authService.logout(httpRequest);

        assertThat(response.success()).isTrue();
        assertThat(response.allSessionsRevoked()).isTrue();
        verify(jwtService).revokeToken(accessToken);
        verify(jwtService).extractUserId(accessToken);
        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    void logout_whenExtractUserIdFails_PropagatesException() {
        String accessToken = "malformed-token";
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + accessToken);
        when(jwtService.extractUserId(accessToken)).thenThrow(new RuntimeException("token parse error"));

        assertThatThrownBy(() -> authService.logout(httpRequest))
                .isInstanceOf(RuntimeException.class);

        verify(jwtService, never()).revokeToken(any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void refreshToken_WithValidRefreshToken_ReturnsNewAuthResponse() {
        String refreshToken = "valid-refresh-token";
        TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        when(jwtService.validateAndRevokeRefreshToken(refreshToken))
                .thenReturn(new JwtService.RefreshTokenValidation(userId, tenantId));

        UserEntity user = UserEntity.builder().id(userId).email("user@example.com").isActive(true).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Tenant tenant = Tenant.builder().id(tenantId).name("Active Tenant").plan(PlanName.FREE).build();
        UserTenant userTenant = UserTenant.builder().tenantId(tenantId).role(RoleName.ADMIN).isActive(true).build();
        
        when(userTenantRepository.findByUserIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(userTenant));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(user.getId(), user.getEmail(), "Name", null, AuthProvider.LOCAL, null, null, null));
        when(tenantMapper.toResponse(any())).thenReturn(new TenantResponse(tenant.getId(), tenant.getName(), "slug", PlanName.FREE, "TECH", null));

        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("new-access");
        verify(jwtService).saveRefreshToken(user, tenantId, "new-refresh");
    }

    @Test
    void register_WhenUserExists_ThrowsDuplicateResourceException() {
        RegisterRequest request = new RegisterRequest("New User", "existing@example.com", "password", "Company", "company", null);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, httpRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void login_WithInvalidCredentials_ThrowsAuthenticationException() {
        LoginRequest request = new LoginRequest("user@example.com", "password");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isAllowed(anyString())).thenReturn(true);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(UserEntity.builder().isActive(true).emailVerifiedAt(ZonedDateTime.now()).build()));
        when(authenticationManager.authenticate(any())).thenThrow(new org.springframework.security.core.AuthenticationException(""){});

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void login_WithUnverifiedEmail_ThrowsAuthorizationException() {
        LoginRequest request = new LoginRequest("unverified@example.com", "password");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isAllowed(anyString())).thenReturn(true);

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(request.email())
                .isActive(true)
                .emailVerifiedAt(null)
                .build();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AuthorizationException.class);

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void refreshToken_WithInvalidToken_PropagatesException() {
        TokenRefreshRequest request = new TokenRefreshRequest("invalid");
        when(jwtService.validateAndRevokeRefreshToken("invalid"))
                .thenThrow(new TokenExpiredException("expired"));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void login_WhenRateLimitExceeded_ThrowsInvalidInputException() {
        LoginRequest request = new LoginRequest("user@example.com", "password");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(rateLimitService.isAllowed(anyString())).thenReturn(false);
        when(rateLimitService.getRemainingAttempts(anyString())).thenReturn(0L);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(InvalidInputException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_SameIpDifferentEmail_HasIndependentRateLimits() {
        LoginRequest request1 = new LoginRequest("user1@example.com", "password");
        LoginRequest request2 = new LoginRequest("user2@example.com", "password");
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.100");

        when(rateLimitService.isAllowed("login:192.168.1.100:user1@example.com")).thenReturn(true);
        when(rateLimitService.isAllowed("login:192.168.1.100:user2@example.com")).thenReturn(true);

        UserEntity user1 = UserEntity.builder().id(UUID.randomUUID()).email("user1@example.com").isActive(true).emailVerifiedAt(ZonedDateTime.now()).build();
        UserEntity user2 = UserEntity.builder().id(UUID.randomUUID()).email("user2@example.com").isActive(true).emailVerifiedAt(ZonedDateTime.now()).build();
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(user2));

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userTenantRepository.findByUserIdAndIsActiveTrue(any())).thenReturn(List.of());
        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(UUID.randomUUID(), "test", "test", null, AuthProvider.LOCAL, null, null, null));

        authService.login(request1, httpRequest);
        authService.login(request2, httpRequest);

        verify(rateLimitService).isAllowed("login:192.168.1.100:user1@example.com");
        verify(rateLimitService).isAllowed("login:192.168.1.100:user2@example.com");
    }
}
