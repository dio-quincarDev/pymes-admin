package auth.pymes.unit;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.entities.Invitation;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.common.models.mappers.InvitationMapper;
import auth.pymes.repositories.InvitationRepository;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.impl.InvitationServiceImpl;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
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
public class InvitationServiceImplTest {

    @Mock
    private UserEntityRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserTenantRepository userTenantRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private InvitationMapper invitationMapper;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @Test
    void getPendingInvitations_WithValidPrincipal_ReturnsPageOfInvitationResponses() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "invited@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        Pageable pageable = PageRequest.of(0, 10);
        Invitation invitation = Invitation.builder().id(UUID.randomUUID()).tenantId(UUID.randomUUID()).email(email).build();
        Page<Invitation> invitationPage = new PageImpl<>(List.of(invitation), pageable, 1);

        when(invitationRepository.findByEmailAndAcceptedAtIsNull(email, pageable)).thenReturn(invitationPage);
        when(invitationMapper.toResponse(any(), any(), any())).thenReturn(mock(InvitationResponse.class));

        Page<InvitationResponse> response = invitationService.getPendingInvitations(pageable, principal);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void createInvitation_WithValidRequest_ReturnsInvitationResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String inviterEmail = "admin@example.com";
        when(principal.getAttribute("email")).thenReturn(inviterEmail);

        UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).build();
        when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));

        UUID tenantId = UUID.randomUUID();
        CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.VIEWER);

        Tenant tenant = Tenant.builder().id(tenantId).name("Test Tenant").maxUsers(5).build();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        UserTenant inviterTenant = UserTenant.builder().role(RoleName.ADMIN).build();
        when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.of(inviterTenant));

        when(invitationRepository.save(any(Invitation.class))).thenReturn(mock(Invitation.class));
        when(invitationMapper.toResponse(any(), any(), any())).thenReturn(mock(InvitationResponse.class));

        InvitationResponse response = invitationService.createInvitation(request, principal);

        assertThat(response).isNotNull();
    }

    @Test
    void acceptInvitation_WithValidToken_ReturnsInvitationResponse() {
        OAuth2User principal = mock(OAuth2User.class);
        String email = "guest@example.com";
        when(principal.getAttribute("email")).thenReturn(email);

        String token = "valid";
        Invitation invitation = Invitation.builder().email(email).expiresAt(ZonedDateTime.now().plusDays(1)).build();
        when(invitationRepository.findByTokenAndAcceptedAtIsNull(token)).thenReturn(Optional.of(invitation));

        UserEntity user = UserEntity.builder().email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        when(invitationMapper.toResponse(any(), any(), any())).thenReturn(mock(InvitationResponse.class));

        InvitationResponse response = invitationService.acceptInvitation(token, principal);

        assertThat(response).isNotNull();
    }
}
