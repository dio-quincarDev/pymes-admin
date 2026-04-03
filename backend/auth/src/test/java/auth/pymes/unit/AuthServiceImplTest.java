package auth.pymes.unit;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.request.TokenRefreshRequest;
import auth.pymes.common.models.dto.response.*;
import auth.pymes.common.models.entities.Invitation;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.repositories.InvitationRepository;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.JwtService;
import auth.pymes.service.impl.AuthServiceImpl;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthorizationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.user.OAuth2User;

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
    private InvitationRepository invitationRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void getCurrentUser_WithValidPrincipal_ReturnsUserResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "test@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("Test User")
                .build();
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserEntityResponse response = authService.getCurrentUser(principal);

        assertThat(response.email()).isEqualTo(email);
        assertThat(response.name()).isEqualTo("Test User");
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserByEmail_WithExistingEmail_ReturnsUserResponse() {
        String email = "test@example.com";
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("Test User")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserEntityResponse response = authService.getUserByEmail(email);

        assertThat(response.email()).isEqualTo(email);
        assertThat(response.name()).isEqualTo("Test User");
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserTenants_WithValidPrincipal_ReturnsPageOfUserTenantResponses() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "test@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Pageable pageable = PageRequest.of(0, 10);
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Tenant Name")
                .slug("tenant-slug")
                .build();
        UserTenant userTenant = UserTenant.builder()
                .tenantId(tenant.getId())
                .tenant(tenant)
                .role(RoleName.ADMIN)
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();
        Page<UserTenant> userTenantPage = new PageImpl<>(List.of(userTenant), pageable, 1);

        when(userTenantRepository.findByUserIdAndIsActiveTrue(user.getId(), pageable))
                .thenReturn(userTenantPage);

        Page<UserTenantResponse> response = authService.getUserTenants(pageable, principal);

        assertThat(response.getContent()).hasSize(1);
        UserTenantResponse first = response.getContent().get(0);
        assertThat(first.tenantId()).isEqualTo(tenant.getId());
        assertThat(first.tenantName()).isEqualTo("Tenant Name");
        assertThat(first.accepted()).isTrue();
    }

    @Test
    void selectTenant_WithValidRequest_ReturnsAuthResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "test@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UUID tenantId = UUID.randomUUID();
        SelectTenantRequest request = new SelectTenantRequest(tenantId);

        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenantId)
                .role(RoleName.ADMIN)
                .isActive(true)
                .build();
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId))
                .thenReturn(Optional.of(userTenant));

        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .name("Tenant Name")
                .slug("slug")
                .isActive(true)
                .build();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        when(jwtService.generateAccessToken(user, tenantId, "ADMIN")).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        AuthResponse response = authService.selectTenant(request, principal);

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.activeTenant().name()).isEqualTo("Tenant Name");
    }

    @Test
    void createTenant_WithValidRequest_ReturnsTenantResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "owner@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        CreateTenantRequest request = new CreateTenantRequest("Mi Empresa", "mi-empresa", "TECHNOLOGY");

        when(tenantRepository.existsBySlug("mi-empresa")).thenReturn(false);

        Tenant savedTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Mi Empresa")
                .slug("mi-empresa")
                .industry("TECHNOLOGY")
                .plan(PlanName.FREE)
                .maxUsers(1)
                .isActive(true)
                .build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        UserTenant savedUserTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(savedTenant.getId())
                .role(RoleName.OWNER)
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();
        when(userTenantRepository.save(any(UserTenant.class))).thenReturn(savedUserTenant);

        TenantResponse response = authService.createTenant(request, principal);

        assertThat(response.name()).isEqualTo("Mi Empresa");
        assertThat(response.slug()).isEqualTo("mi-empresa");
        assertThat(response.plan()).isEqualTo(PlanName.FREE);
    }

    @Test
    void logout_WithValidToken_ReturnsLogoutResponseAndRevokesToken() {
        String accessToken = "valid-access-token";

        doNothing().when(jwtService).revokeToken(accessToken);

        LogoutResponse response = authService.logout(accessToken);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Logout successful");
        verify(jwtService).revokeToken(accessToken);
    }

    @Test
    void refreshToken_WithValidRefreshToken_ReturnsNewAuthResponse() {
        String refreshToken = "valid-refresh-token";
        TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);

        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        when(jwtService.extractUserId(refreshToken)).thenReturn(userId);
        when(jwtService.extractEmail(refreshToken)).thenReturn(email);

        UserEntity user = UserEntity.builder()
                .id(userId)
                .email(email)
                .isActive(true)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        List<UserTenant> userTenants = List.of(
                UserTenant.builder()
                        .tenantId(UUID.randomUUID())
                        .tenant(Tenant.builder().name("Active Tenant").build())
                        .role(RoleName.ADMIN)
                        .isActive(true)
                        .build()
        );
        when(userTenantRepository.findByUserIdAndIsActiveTrue(userId)).thenReturn(userTenants);

        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        when(jwtService.generateAccessToken(user, userTenants.get(0).getTenantId(), "ADMIN"))
                .thenReturn(newAccessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(newRefreshToken);

        Tenant activeTenant = userTenants.get(0).getTenant();
        when(tenantRepository.findById(userTenants.get(0).getTenantId())).thenReturn(Optional.of(activeTenant));

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo(newAccessToken);
        assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
        assertThat(response.user().email()).isEqualTo(email);
        assertThat(response.activeTenant().name()).isEqualTo("Active Tenant");
    }

    @Test
    void getPendingInvitations_WithValidPrincipal_ReturnsPageOfInvitationResponses() {
        // Arrange
        OAuth2User principal = mock(OAuth2User.class);
        String email = "invited@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        Pageable pageable = PageRequest.of(0, 10);
        UUID tenantId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email(email)
                .role(RoleName.VIEWER)
                .invitedBy(inviterId)
                .createdAt(ZonedDateTime.now())
                .expiresAt(ZonedDateTime.now().plusDays(7))
                .build();
        
        Page<Invitation> invitationPage = new PageImpl<>(List.of(invitation), pageable, 1);

        when(invitationRepository.findByEmailAndAcceptedAtIsNull(email, pageable))
                .thenReturn(invitationPage);

        Tenant tenant = Tenant.builder().id(tenantId).name("Test Tenant").build();
        UserEntity inviter = UserEntity.builder().id(inviterId).name("Inviter User").build();
        
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviter));

        // Act
        Page<InvitationResponse> response = authService.getPendingInvitations(pageable, principal);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        InvitationResponse first = response.getContent().get(0);
        assertThat(first.email()).isEqualTo(email);
        assertThat(first.tenantName()).isEqualTo("Test Tenant");
        assertThat(first.invitedBy()).isEqualTo("Inviter User");
        assertThat(first.accepted()).isFalse();
        verify(invitationRepository).findByEmailAndAcceptedAtIsNull(email, pageable);
    }

    @Test
    void createInvitation_WithValidRequest_ReturnsInvitationResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String inviterEmail = "admin@example.com";
        when(principal.getAttribute("email")).thenReturn(inviterEmail);

        UserEntity inviter = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(inviterEmail)
                .build();
        when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));

        UUID tenantId = UUID.randomUUID();
        CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.VIEWER);

        Tenant tenant = Tenant.builder().id(tenantId).name("Test Tenant").build();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        UserTenant inviterTenant = UserTenant.builder()
                .userId(inviter.getId())
                .tenantId(tenantId)
                .role(RoleName.ADMIN)
                .isActive(true)
                .build();
        when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId))
                .thenReturn(Optional.of(inviterTenant));

        when(invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(tenantId, "guest@example.com"))
                .thenReturn(false);
        when(userRepository.findByEmail("guest@example.com")).thenReturn(Optional.empty());

        Invitation savedInvitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("guest@example.com")
                .role(RoleName.VIEWER)
                .invitedBy(inviter.getId())
                .token(UUID.randomUUID().toString())
                .expiresAt(ZonedDateTime.now().plusDays(7))
                .build();
        when(invitationRepository.save(any(Invitation.class))).thenReturn(savedInvitation);

        // Mappers: tenantRepository.findById e userRepository.findById para la respuesta
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(inviter.getId())).thenReturn(Optional.of(inviter));

        InvitationResponse response = authService.createInvitation(request, principal);

        assertThat(response.email()).isEqualTo("guest@example.com");
        assertThat(response.role()).isEqualTo(RoleName.VIEWER);
        assertThat(response.tenantName()).isEqualTo("Test Tenant");
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    void acceptInvitation_WithValidToken_ReturnsInvitationResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "guest@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        String invitationToken = "valid-token";
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .email(email)
                .role(RoleName.VIEWER)
                .invitedBy(UUID.randomUUID())
                .token(invitationToken)
                .expiresAt(ZonedDateTime.now().plusDays(1))
                .build();
        when(invitationRepository.findByTokenAndAcceptedAtIsNull(invitationToken))
                .thenReturn(Optional.of(invitation));

        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(userTenantRepository.save(any(UserTenant.class))).thenReturn(null);
        when(invitationRepository.save(any(Invitation.class))).thenReturn(invitation);

        // Para mapToInvitationResponse
        Tenant tenant = Tenant.builder().id(invitation.getTenantId()).name("Test Tenant").build();
        UserEntity inviter = UserEntity.builder().id(invitation.getInvitedBy()).name("Inviter").build();
        when(tenantRepository.findById(invitation.getTenantId())).thenReturn(Optional.of(tenant));
        when(userRepository.findById(invitation.getInvitedBy())).thenReturn(Optional.of(inviter));

        InvitationResponse response = authService.acceptInvitation(invitationToken, principal);

        assertThat(response.email()).isEqualTo(email);
        assertThat(response.accepted()).isTrue();
        verify(userTenantRepository).save(any(UserTenant.class));
        verify(invitationRepository).save(invitation);
    }

    @Test
    void cancelInvitation_WithValidIdAndOwner_DeletesInvitation() {
        OAuth2User principal = mock(OAuth2User.class);
        String inviterEmail = "admin@example.com";
        when(principal.getAttribute("email")).thenReturn(inviterEmail);

        UserEntity inviter = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(inviterEmail)
                .build();
        when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));

        UUID invitationId = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .id(invitationId)
                .invitedBy(inviter.getId())
                .tenantId(UUID.randomUUID())
                .build();
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        doNothing().when(invitationRepository).delete(invitation);

        authService.cancelInvitation(invitationId, principal);

        verify(invitationRepository).delete(invitation);
    }

    @Test
    void selectTenant_WhenUserNotInTenant_ThrowsAuthorizationException() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn("user@example.com");
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).build();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        SelectTenantRequest request = new SelectTenantRequest(UUID.randomUUID());
        when(userTenantRepository.findByUserIdAndTenantId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.selectTenant(request, principal))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining(CodigoError.USER_NOT_IN_TENANT.getMensaje(request.tenantId()));
    }

    @Test
    void selectTenant_WhenUserTenantInactive_ThrowsAuthorizationException() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn("user@example.com");
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).build();
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

        UUID tenantId = UUID.randomUUID();
        SelectTenantRequest request = new SelectTenantRequest(tenantId);

        UserTenant userTenant = UserTenant.builder()
                .isActive(false)
                .build();
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId))
                .thenReturn(Optional.of(userTenant));

        assertThatThrownBy(() -> authService.selectTenant(request, principal))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("User access to tenant is inactive");
    }
}
