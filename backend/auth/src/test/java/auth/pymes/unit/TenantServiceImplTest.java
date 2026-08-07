package auth.pymes.unit;

import auth.pymes.common.models.dto.request.CreateTenantRequest;
import auth.pymes.common.models.dto.request.SelectTenantRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.common.models.dto.response.TenantResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.dto.response.UserTenantResponse;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.common.models.mappers.TenantMapper;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.JwtService;
import auth.pymes.service.impl.TenantServiceImpl;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests - TenantServiceImpl")
public class TenantServiceImplTest {

    @Mock
    private UserEntityRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserTenantRepository userTenantRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TenantServiceImpl tenantService;

    @Test
    @DisplayName("getUserTenants - Con Principal OAuth2User → Retorna página de tenants")
    void getUserTenants_WithOAuth2User_ReturnsPage() {
        // Arrange
        String email = "oauth2@example.com";
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(oAuth2User.getAttribute("email")).thenReturn(email);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(oAuth2User);

        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Pageable pageable = PageRequest.of(0, 10);
        Tenant tenant = Tenant.builder().id(UUID.randomUUID()).name("OAuth2 Corp").slug("oauth2-corp").build();
        UserTenant userTenant = UserTenant.builder().tenantId(tenant.getId()).tenant(tenant).role(RoleName.OWNER).isActive(true).build();
        Page<UserTenant> userTenantPage = new PageImpl<>(List.of(userTenant), pageable, 1);

        when(userTenantRepository.findByUserIdAndIsActiveTrue(user.getId(), pageable)).thenReturn(userTenantPage);

        // Act
        Page<UserTenantResponse> response = tenantService.getUserTenants(pageable, auth);

        // Assert
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).tenantName()).isEqualTo("OAuth2 Corp");
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("createTenant - Con Principal UserEntity (JWT) → Crea tenant exitosamente")
    void createTenant_WithUserEntityPrincipal_Success() {
        // Arrange
        String email = "jwt@example.com";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.countByUserIdAndRole(user.getId(), RoleName.OWNER)).thenReturn(0L);
        when(tenantRepository.existsBySlug("new-space")).thenReturn(false);

        Tenant savedTenant = Tenant.builder().id(UUID.randomUUID()).name("New Space").slug("new-space").plan(PlanName.FREE).build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(tenantMapper.toResponse(any())).thenReturn(new TenantResponse(savedTenant.getId(), "New Space", "new-space", PlanName.FREE, "TECH", null));

        CreateTenantRequest request = new CreateTenantRequest("New Space", "new-space", "TECH");

        // Act
        TenantResponse response = tenantService.createTenant(request, auth);

        // Assert
        assertThat(response.name()).isEqualTo("New Space");
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("selectTenant - Con Principal String (Fallback) → Retorna AuthResponse")
    void selectTenant_WithStringPrincipal_Success() {
        // Arrange
        String email = "fallback@example.com";
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("NotAUserEntityOrOAuth2User");
        when(auth.getName()).thenReturn(email);

        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UUID tenantId = UUID.randomUUID();
        SelectTenantRequest request = new SelectTenantRequest(tenantId);

        UserTenant userTenant = UserTenant.builder().userId(user.getId()).tenantId(tenantId).role(RoleName.ADMIN).isActive(true).build();
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId)).thenReturn(Optional.of(userTenant));

        Tenant tenant = Tenant.builder().id(tenantId).name("Select Corp").plan(PlanName.FREE).isActive(true).build();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(user.getId(), email, "Name", null, AuthProvider.LOCAL, null, null, null));
        when(tenantMapper.toResponse(any())).thenReturn(new TenantResponse(tenant.getId(), tenant.getName(), "select-corp", PlanName.FREE, "TECH", null));

        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        // Act
        AuthResponse response = tenantService.selectTenant(request, auth);

        // Assert
        assertThat(response.accessToken()).isEqualTo("new-access-token");
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("createTenant - Límite de Plan FREE excedido → Lanza InvalidInputException")
    void createTenant_LimitExceeded_ThrowsException() {
        // Arrange
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("owner@example.com").build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(userTenantRepository.countByUserIdAndRole(user.getId(), RoleName.OWNER)).thenReturn(1L);

        CreateTenantRequest request = new CreateTenantRequest("Too Many", "too-many", "TECH");

        // Act & Assert
        assertThatThrownBy(() -> tenantService.createTenant(request, auth))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("FREE tenants");
    }

    @Test
    @DisplayName("getEmailFromAuthentication - Authentication null → Lanza AuthorizationException")
    void getEmail_NullAuth_ThrowsException() {
        assertThatThrownBy(() -> tenantService.getUserTenants(PageRequest.of(0, 10), null))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("No authentication found");
    }

    @Test
    @DisplayName("getEmailFromAuthentication - Principal no soportado → Lanza fallback a getName()")
    void getEmail_UnknownPrincipal_UsesGetName() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new Object());
        when(auth.getName()).thenReturn("fallback@example.com");

        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("fallback@example.com").build();
        when(userRepository.findByEmail("fallback@example.com")).thenReturn(Optional.of(user));

        Pageable pageable = PageRequest.of(0, 10);
        when(userTenantRepository.findByUserIdAndIsActiveTrue(any(), any())).thenReturn(new PageImpl<>(List.of()));

        // Act
        tenantService.getUserTenants(pageable, auth);

        // Assert
        verify(auth).getName();
        verify(userRepository).findByEmail("fallback@example.com");
    }

    @Test
    @DisplayName("shutdown - Owner hace shutdown exitosamente")
    void shutdown_OwnerSuccess() {
        // Arrange
        String email = "owner@example.com";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).name("My Company").slug("my-company").isActive(true).build();

        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenantId)
                .role(RoleName.OWNER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId)).thenReturn(Optional.of(userTenant));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        // Act
        tenantService.shutdown(tenantId, auth);

        // Assert
        verify(tenantRepository).save(argThat(t -> !t.getIsActive()));
    }

    @Test
    @DisplayName("shutdown - No owner lanza AuthorizationException")
    void shutdown_NotOwner_ThrowsException() {
        // Arrange
        String email = "admin@example.com";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        UUID tenantId = UUID.randomUUID();
        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenantId)
                .role(RoleName.ADMIN)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId)).thenReturn(Optional.of(userTenant));

        // Act & Assert
        assertThatThrownBy(() -> tenantService.shutdown(tenantId, auth))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("shutdown - Usuario no miembro lanza AuthorizationException")
    void shutdown_NotMember_ThrowsException() {
        // Arrange
        String email = "outsider@example.com";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        UUID tenantId = UUID.randomUUID();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tenantService.shutdown(tenantId, auth))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("not belong");
    }

    @Test
    @DisplayName("shutdown - Tenant inactivo lanza AuthorizationException")
    void shutdown_AlreadyInactive_ThrowsException() {
        // Arrange
        String email = "owner@example.com";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.builder().id(tenantId).name("My Company").isActive(false).build();

        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(tenantId)
                .role(RoleName.OWNER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId)).thenReturn(Optional.of(userTenant));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        // Act & Assert
        assertThatThrownBy(() -> tenantService.shutdown(tenantId, auth))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("already inactive");
    }
}
