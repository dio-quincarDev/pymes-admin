package auth.pymes.unit;

import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.impl.UserServiceImpl;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private UserTenantRepository userTenantRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getCurrentUser_WithValidPrincipal_ReturnsUserResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "test@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UUID tenantId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("Test User")
                .build();
        UserTenant ut = UserTenant.builder()
                .tenantId(tenantId)
                .role(RoleName.OWNER)
                .build();
        Tenant tenant = Tenant.builder()
                .plan(PlanName.FREE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of(ut));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        UserEntityResponse response = userService.getCurrentUser(principal);

        assertThat(response.email()).isEqualTo(email);
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.role()).isEqualTo("OWNER");
        assertThat(response.plan()).isEqualTo("FREE");
    }

    @Test
    void getCurrentUser_WithUserDetailsPrincipal_ReturnsUserResponse() {
        String email = "userdetails@example.com";
        UserDetails principal = User.withUsername(email).password("ignored").authorities(Collections.emptyList()).build();

        UUID tenantId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("UserDetails User")
                .build();
        UserTenant ut = UserTenant.builder()
                .tenantId(tenantId)
                .role(RoleName.ADMIN)
                .build();
        Tenant tenant = Tenant.builder()
                .plan(PlanName.STARTER)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of(ut));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        UserEntityResponse response = userService.getCurrentUser(principal);

        assertThat(response.email()).isEqualTo(email);
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.plan()).isEqualTo("STARTER");
    }

    @Test
    void getCurrentUser_WithStringPrincipal_ReturnsUserResponse() {
        String email = "string@example.com";

        UUID tenantId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .name("String Principal User")
                .build();
        UserTenant ut = UserTenant.builder()
                .tenantId(tenantId)
                .role(RoleName.VIEWER)
                .build();
        Tenant tenant = Tenant.builder()
                .plan(PlanName.PRO)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userTenantRepository.findByUserIdAndIsActiveTrue(user.getId())).thenReturn(List.of(ut));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        UserEntityResponse response = userService.getCurrentUser(email);

        assertThat(response.email()).isEqualTo(email);
        assertThat(response.tenantId()).isEqualTo(tenantId);
        assertThat(response.role()).isEqualTo("VIEWER");
        assertThat(response.plan()).isEqualTo("PRO");
    }

    @Test
    void getUserByEmail_WithExistingEmail_ReturnsUserResponse() {
        String email = "test@example.com";
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserEntityResponse response = userService.getUserByEmail(email);

        assertThat(response.email()).isEqualTo(email);
    }

    @Test
    void getUserByEmail_WhenEmailNotFound_ThrowsResourceNotFoundException() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
