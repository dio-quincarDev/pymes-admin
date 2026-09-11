package auth.pymes.unit;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.impl.MemberServiceImpl;
import auth.pymes.utils.exception.auth.AuthorizationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceImplNewLogicTest {

    @Mock private UserEntityRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserTenantRepository userTenantRepository;
    @Mock private UserMapper userMapper;
    @InjectMocks private MemberServiceImpl memberService;

    @Test
    void adminCannotUpdateRole() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn("admin@ex.com");
        UserEntity admin = UserEntity.builder().id(UUID.randomUUID()).email("admin@ex.com").build();
        when(userRepository.findByEmail("admin@ex.com")).thenReturn(Optional.of(admin));
        UUID tenantId = UUID.randomUUID();
        UserTenant adminRel = UserTenant.builder().role(RoleName.ADMIN).build();
        when(userTenantRepository.findByUserIdAndTenantId(admin.getId(), tenantId)).thenReturn(Optional.of(adminRel));

        assertThatThrownBy(() -> memberService.updateUserRole(tenantId, UUID.randomUUID(), "VIEWER", principal))
                .isInstanceOf(AuthorizationException.class);
        verify(userTenantRepository, never()).save(any());
    }

    @Test
    void ownerCannotAssignOwnerRole() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn("owner@ex.com");
        UserEntity owner = UserEntity.builder().id(UUID.randomUUID()).email("owner@ex.com").build();
        when(userRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(owner));
        UUID tenantId = UUID.randomUUID();
        UserTenant ownerRel = UserTenant.builder().role(RoleName.OWNER).build();
        when(userTenantRepository.findByUserIdAndTenantId(owner.getId(), tenantId)).thenReturn(Optional.of(ownerRel));
        UUID targetId = UUID.randomUUID();
        UserTenant targetRel = UserTenant.builder().user(UserEntity.builder().id(targetId).build()).userId(targetId).role(RoleName.VIEWER).build();
        when(userTenantRepository.findByUserIdAndTenantId(targetId, tenantId)).thenReturn(Optional.of(targetRel));

        assertThatThrownBy(() -> memberService.updateUserRole(tenantId, targetId, "OWNER", principal))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Cannot assign OWNER");
    }

    @Test
    void ownerCannotModifyOwnerTarget() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn("owner@ex.com");
        UserEntity owner = UserEntity.builder().id(UUID.randomUUID()).email("owner@ex.com").build();
        when(userRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(owner));
        UUID tenantId = UUID.randomUUID();
        UserTenant ownerRel = UserTenant.builder().role(RoleName.OWNER).build();
        when(userTenantRepository.findByUserIdAndTenantId(owner.getId(), tenantId)).thenReturn(Optional.of(ownerRel));
        UUID targetId = UUID.randomUUID();
        UserTenant targetOwner = UserTenant.builder().user(UserEntity.builder().id(targetId).build()).userId(targetId).role(RoleName.OWNER).build();
        when(userTenantRepository.findByUserIdAndTenantId(targetId, tenantId)).thenReturn(Optional.of(targetOwner));

        assertThatThrownBy(() -> memberService.updateUserRole(tenantId, targetId, "ADMIN", principal))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void ownerCanDemoteAdminToViewer() {
        OAuth2User principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn("owner@ex.com");
        UserEntity owner = UserEntity.builder().id(UUID.randomUUID()).email("owner@ex.com").build();
        when(userRepository.findByEmail("owner@ex.com")).thenReturn(Optional.of(owner));
        UUID tenantId = UUID.randomUUID();
        UserTenant ownerRel = UserTenant.builder().role(RoleName.OWNER).build();
        when(userTenantRepository.findByUserIdAndTenantId(owner.getId(), tenantId)).thenReturn(Optional.of(ownerRel));
        UUID targetId = UUID.randomUUID();
        UserEntity targetUser = UserEntity.builder().id(targetId).build();
        UserTenant targetRel = UserTenant.builder().user(targetUser).userId(targetId).role(RoleName.ADMIN).build();
        when(userTenantRepository.findByUserIdAndTenantId(targetId, tenantId)).thenReturn(Optional.of(targetRel));
        when(userMapper.toResponse(any())).thenReturn(null);

        // should not throw
        var resp = memberService.updateUserRole(tenantId, targetId, "VIEWER", principal);
        assertThat(targetRel.getRole()).isEqualTo(RoleName.VIEWER);
    }
}
