package auth.pymes.unit;

import auth.pymes.common.models.dto.response.MemberResponse;
import auth.pymes.common.models.dto.response.UserEntityResponse;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.common.models.mappers.UserMapper;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.impl.MemberServiceImpl;
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
public class MemberServiceImplTest {

    @Mock
    private UserEntityRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserTenantRepository userTenantRepository;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MemberServiceImpl memberService;

    @Test
    void getTenantUsers_WithAdminPermissions_ReturnsPage() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "admin@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity admin = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(admin));

        UUID tenantId = UUID.randomUUID();
        UserTenant adminRelation = UserTenant.builder().role(RoleName.ADMIN).build();
        when(userTenantRepository.findByUserIdAndTenantId(admin.getId(), tenantId)).thenReturn(Optional.of(adminRelation));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(mock(Tenant.class)));

        Pageable pageable = PageRequest.of(0, 10);
        UserEntity member = UserEntity.builder().id(UUID.randomUUID()).name("Member").build();
        UserTenant memberRelation = UserTenant.builder().user(member).role(RoleName.VIEWER).build();
        Page<UserTenant> page = new PageImpl<>(List.of(memberRelation));
        
        when(userTenantRepository.findByTenantIdAndIsActiveTrue(tenantId, pageable)).thenReturn(page);
        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(member.getId(), "m@ex.com", "Member", null, AuthProvider.LOCAL, null, null, null));

        Page<MemberResponse> response = memberService.getTenantUsers(tenantId, pageable, principal);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).role()).isEqualTo(RoleName.VIEWER);
    }

    @Test
    void updateUserRole_WithHierarchy_UpdatesSuccessfully() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "owner@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity owner = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));

        UUID tenantId = UUID.randomUUID();
        UserTenant ownerRelation = UserTenant.builder().role(RoleName.OWNER).build();
        when(userTenantRepository.findByUserIdAndTenantId(owner.getId(), tenantId)).thenReturn(Optional.of(ownerRelation));

        UUID targetUserId = UUID.randomUUID();
        UserEntity targetUser = UserEntity.builder().id(targetUserId).build();
        UserTenant targetRelation = UserTenant.builder().user(targetUser).userId(targetUserId).role(RoleName.VIEWER).build();
        when(userTenantRepository.findByUserIdAndTenantId(targetUserId, tenantId)).thenReturn(Optional.of(targetRelation));
        
        when(userMapper.toResponse(any())).thenReturn(new UserEntityResponse(targetUserId, "t@ex.com", "Target", null, AuthProvider.LOCAL, null, null, null));

        MemberResponse response = memberService.updateUserRole(tenantId, targetUserId, "ADMIN", principal);

        assertThat(response).isNotNull();
        assertThat(targetRelation.getRole()).isEqualTo(RoleName.ADMIN);
    }

    @Test
    void deleteUser_ByOwner_RemovesSuccessfully() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "owner@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        UserEntity owner = UserEntity.builder().id(UUID.randomUUID()).email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(owner));

        UUID tenantId = UUID.randomUUID();
        UserTenant ownerRelation = UserTenant.builder().role(RoleName.OWNER).build();
        when(userTenantRepository.findByUserIdAndTenantId(owner.getId(), tenantId)).thenReturn(Optional.of(ownerRelation));

        UUID targetUserId = UUID.randomUUID();
        UserTenant targetRelation = UserTenant.builder().role(RoleName.ADMIN).build();
        when(userTenantRepository.findByUserIdAndTenantId(targetUserId, tenantId)).thenReturn(Optional.of(targetRelation));

        memberService.deleteUserFromTenant(tenantId, targetUserId, principal);

        verify(userTenantRepository).delete(targetRelation);
    }
}
