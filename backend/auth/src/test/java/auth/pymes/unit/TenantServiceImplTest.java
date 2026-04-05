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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    void getUserTenants_WithValidPrincipal_ReturnsPageOfUserTenantResponses() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "test@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Pageable pageable = PageRequest.of(0, 10);
        Tenant tenant = Tenant.builder().id(UUID.randomUUID()).name("Tenant Name").slug("slug").build();
        UserTenant userTenant = UserTenant.builder().tenantId(tenant.getId()).tenant(tenant).role(RoleName.ADMIN).isActive(true).build();
        Page<UserTenant> userTenantPage = new PageImpl<>(List.of(userTenant), pageable, 1);

        when(userTenantRepository.findByUserIdAndIsActiveTrue(user.getId(), pageable)).thenReturn(userTenantPage);

        Page<UserTenantResponse> response = tenantService.getUserTenants(pageable, principal);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).tenantName()).isEqualTo("Tenant Name");
    }

    @Test
    void selectTenant_WithValidRequest_ReturnsAuthResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "test@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UUID tenantId = UUID.randomUUID();
        SelectTenantRequest request = new SelectTenantRequest(tenantId);

        UserTenant userTenant = UserTenant.builder().userId(user.getId()).tenantId(tenantId).role(RoleName.ADMIN).isActive(true).build();
        when(userTenantRepository.findByUserIdAndTenantId(user.getId(), tenantId)).thenReturn(Optional.of(userTenant));

        Tenant tenant = Tenant.builder().id(tenantId).name("Tenant Name").plan(PlanName.FREE).isActive(true).build();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(user.getId(), email, "Name", null, AuthProvider.LOCAL));
        when(tenantMapper.toResponse(any())).thenReturn(new TenantResponse(tenant.getId(), tenant.getName(), "slug", PlanName.FREE, "TECH", null));

        when(jwtService.generateAccessToken(any(), any(), any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        AuthResponse response = tenantService.selectTenant(request, principal);

        assertThat(response.accessToken()).isEqualTo("access");
    }

    @Test
    void createTenant_WithValidRequest_ReturnsTenantResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "owner@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        CreateTenantRequest request = new CreateTenantRequest("Mi Empresa", "mi-empresa", "TECHNOLOGY");
        when(tenantRepository.existsBySlug("mi-empresa")).thenReturn(false);

        Tenant savedTenant = Tenant.builder().id(UUID.randomUUID()).name("Mi Empresa").plan(PlanName.FREE).build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(tenantMapper.toResponse(any())).thenReturn(new TenantResponse(savedTenant.getId(), "Mi Empresa", "mi-empresa", PlanName.FREE, "TECH", null));

        TenantResponse response = tenantService.createTenant(request, principal);

        assertThat(response.name()).isEqualTo("Mi Empresa");
    }
}
