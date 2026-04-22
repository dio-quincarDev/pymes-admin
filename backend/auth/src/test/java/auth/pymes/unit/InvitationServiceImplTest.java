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
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService Unit Tests")
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
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(invitationService, "frontendUrl", "http://localhost:9000");
        ReflectionTestUtils.setField(invitationService, "fromEmail", "noreply@pymes.com");
    }

    @Nested
    @DisplayName("Identity Resolution Tests (Hybrid Support)")
    class IdentityResolutionTests {

        @Test
        @DisplayName("Extract email from OAuth2User principal")
        void extractEmail_FromOAuth2User() {
            OAuth2User principal = mock(OAuth2User.class);
            when(principal.getAttribute("email")).thenReturn("social@example.com");
            
            Pageable pageable = PageRequest.of(0, 10);
            when(invitationRepository.findByEmailAndAcceptedAtIsNull(eq("social@example.com"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            invitationService.getPendingInvitations(pageable, principal);
            
            verify(invitationRepository).findByEmailAndAcceptedAtIsNull("social@example.com", pageable);
        }

        @Test
        @DisplayName("Extract email from UserDetails principal (Local)")
        void extractEmail_FromUserDetails() {
            UserDetails principal = mock(UserDetails.class);
            when(principal.getUsername()).thenReturn("local@example.com");
            
            Pageable pageable = PageRequest.of(0, 10);
            when(invitationRepository.findByEmailAndAcceptedAtIsNull(eq("local@example.com"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            invitationService.getPendingInvitations(pageable, principal);
            
            verify(invitationRepository).findByEmailAndAcceptedAtIsNull("local@example.com", pageable);
        }

        @Test
        @DisplayName("Extract email from String principal (Direct)")
        void extractEmail_FromString() {
            String principal = "direct@example.com";
            
            Pageable pageable = PageRequest.of(0, 10);
            when(invitationRepository.findByEmailAndAcceptedAtIsNull(eq("direct@example.com"), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            invitationService.getPendingInvitations(pageable, principal);
            
            verify(invitationRepository).findByEmailAndAcceptedAtIsNull("direct@example.com", pageable);
        }

        @Test
        @DisplayName("Extract email from invalid principal → throws AuthorizationException")
        void extractEmail_InvalidPrincipal() {
            Object invalidPrincipal = new Object();
            
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> invitationService.getPendingInvitations(pageable, invalidPrincipal))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Could not extract email");
        }

        @Test
        @DisplayName("Get pending invitations with enrichment → returns responses with tenant and inviter")
        void getPendingInvitations_Success() {
            String email = "user@example.com";
            Pageable pageable = PageRequest.of(0, 10);
            UUID tenantId = UUID.randomUUID();
            UUID inviterId = UUID.randomUUID();
            
            Invitation invitation = Invitation.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .email(email)
                    .invitedBy(inviterId)
                    .build();
            Tenant tenant = Tenant.builder().id(tenantId).name("Acme Corp").build();
            UserEntity inviter = UserEntity.builder().id(inviterId).name("Admin").build();

            when(invitationRepository.findByEmailAndAcceptedAtIsNull(eq(email), any()))
                    .thenReturn(new PageImpl<>(List.of(invitation)));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(userRepository.findById(inviterId)).thenReturn(Optional.of(inviter));
            when(invitationMapper.toResponse(any(), any(), any())).thenReturn(new InvitationResponse(
                    invitation.getId(),
                    tenantId,
                    "Acme Corp",
                    email,
                    RoleName.VIEWER,
                    "Admin",
                    null,
                    null,
                    false
            ));

            invitationService.getPendingInvitations(pageable, email);

            verify(invitationMapper).toResponse(invitation, tenant, inviter);
        }
    }

    @Nested
    @DisplayName("createInvitation")
    class CreateInvitationTests {

        @Test
        @DisplayName("Successful invitation → sends email and returns response")
        void createInvitation_Success() {
            String inviterEmail = "admin@example.com";
            UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).name("Admin").build();
            UUID tenantId = UUID.randomUUID();
            Tenant tenant = Tenant.builder().id(tenantId).name("Acme Corp").maxUsers(5).build();
            UserTenant inviterTenant = UserTenant.builder().role(RoleName.OWNER).build();
            CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.ADMIN);
            Invitation savedInvitation = Invitation.builder()
                    .tenantId(tenantId)
                    .email(request.email())
                    .role(request.role())
                    .invitedBy(inviter.getId())
                    .build();

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.of(inviterTenant));
            when(userTenantRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(1L);
            when(invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(tenantId, request.email())).thenReturn(false);
            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
            when(invitationRepository.save(any())).thenReturn(savedInvitation);
            when(invitationMapper.toResponse(any(), any(), any())).thenReturn(new InvitationResponse(
                    UUID.randomUUID(),
                    tenantId,
                    "Acme Corp",
                    request.email(),
                    RoleName.ADMIN,
                    "Admin",
                    null,
                    null,
                    false
            ));
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            invitationService.createInvitation(request, inviterEmail);

            verify(invitationRepository).save(any(Invitation.class));
        }

        @Test
        @DisplayName("Role hierarchy violation → throws AuthorizationException")
        void createInvitation_InsufficientRolePower() {
            String inviterEmail = "admin@example.com";
            UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).build();
            UUID tenantId = UUID.randomUUID();
            UserTenant inviterTenant = UserTenant.builder().role(RoleName.ADMIN).build();
            // Un ADMIN intenta invitar a un OWNER (ADMIN no tiene más poder que OWNER)
            CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.OWNER);

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(Tenant.builder().id(tenantId).build()));
            when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.of(inviterTenant));

            assertThatThrownBy(() -> invitationService.createInvitation(request, inviterEmail))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Cannot invite a user with role equal or higher than yours");
        }

        @Test
        @DisplayName("Tenant max users reached → throws DuplicateResourceException")
        void createInvitation_MaxUsersReached() {
            String inviterEmail = "admin@example.com";
            UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).build();
            UUID tenantId = UUID.randomUUID();
            Tenant tenant = Tenant.builder().id(tenantId).maxUsers(2).build();
            UserTenant inviterTenant = UserTenant.builder().role(RoleName.OWNER).build();
            CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.VIEWER);

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.of(inviterTenant));
            when(userTenantRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(2L);

            assertThatThrownBy(() -> invitationService.createInvitation(request, inviterEmail))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Tenant has reached maximum number of users (2)");
        }

        @Test
        @DisplayName("User not in tenant → throws AuthorizationException")
        void createInvitation_UserNotInTenant() {
            String inviterEmail = "outsider@example.com";
            UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).build();
            UUID tenantId = UUID.randomUUID();
            CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.VIEWER);

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(Tenant.builder().id(tenantId).build()));
            when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invitationService.createInvitation(request, inviterEmail))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("does not belong to tenant");
        }

        @Test
        @DisplayName("Email already invited → throws DuplicateResourceException")
        void createInvitation_EmailAlreadyInvited() {
            String inviterEmail = "owner@example.com";
            UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).build();
            UUID tenantId = UUID.randomUUID();
            Tenant tenant = Tenant.builder().id(tenantId).maxUsers(5).build();
            UserTenant inviterTenant = UserTenant.builder().role(RoleName.OWNER).build();
            CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.VIEWER);

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.of(inviterTenant));
            when(userTenantRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(1L);
            when(invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(tenantId, request.email())).thenReturn(true);

            assertThatThrownBy(() -> invitationService.createInvitation(request, inviterEmail))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("has already been invited");
        }

        @Test
        @DisplayName("User already member of tenant → throws DuplicateResourceException")
        void createInvitation_UserAlreadyMember() {
            String inviterEmail = "owner@example.com";
            UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).build();
            UUID tenantId = UUID.randomUUID();
            Tenant tenant = Tenant.builder().id(tenantId).maxUsers(5).build();
            UserTenant inviterTenant = UserTenant.builder().role(RoleName.OWNER).build();
            UserEntity existingUser = UserEntity.builder().id(UUID.randomUUID()).email("guest@example.com").build();
            CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.VIEWER);

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.of(inviterTenant));
            when(userTenantRepository.countByTenantIdAndIsActiveTrue(tenantId)).thenReturn(1L);
            when(invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(tenantId, request.email())).thenReturn(false);
            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
            when(userTenantRepository.findByUserIdAndTenantId(existingUser.getId(), tenantId)).thenReturn(Optional.of(UserTenant.builder().build()));

            assertThatThrownBy(() -> invitationService.createInvitation(request, inviterEmail))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("already a member");
        }

        @Test
        @DisplayName("Viewer with insufficient role → throws AuthorizationException")
        void createInvitation_ViewerInsufficientRole() {
            String inviterEmail = "viewer@example.com";
            UserEntity inviter = UserEntity.builder().id(UUID.randomUUID()).email(inviterEmail).build();
            UUID tenantId = UUID.randomUUID();
            UserTenant inviterTenant = UserTenant.builder().role(RoleName.VIEWER).build();
            CreateInvitationRequest request = new CreateInvitationRequest(tenantId, "guest@example.com", RoleName.VIEWER);

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(Tenant.builder().id(tenantId).build()));
            when(userTenantRepository.findByUserIdAndTenantId(inviter.getId(), tenantId)).thenReturn(Optional.of(inviterTenant));

            assertThatThrownBy(() -> invitationService.createInvitation(request, inviterEmail))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    @Nested
    @DisplayName("acceptInvitation")
    class AcceptInvitationTests {

        @Test
        @DisplayName("Expired invitation → throws InvalidInputException")
        void acceptInvitation_Expired() {
            String token = "expired-token";
            String email = "guest@example.com";
            Invitation invitation = Invitation.builder()
                    .email(email)
                    .expiresAt(ZonedDateTime.now().minusDays(1))
                    .build();

            when(invitationRepository.findByTokenAndAcceptedAtIsNull(token)).thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> invitationService.acceptInvitation(token, email))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessageContaining("Invitation has expired");
        }

        @Test
        @DisplayName("Email mismatch → throws AuthorizationException")
        void acceptInvitation_EmailMismatch() {
            String token = "valid-token";
            String authenticatedEmail = "wrong@example.com";
            Invitation invitation = Invitation.builder()
                    .email("right@example.com")
                    .build();

            when(invitationRepository.findByTokenAndAcceptedAtIsNull(token)).thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> invitationService.acceptInvitation(token, authenticatedEmail))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Invitation email does not match authenticated user");
        }

        @Test
        @DisplayName("Invitation not found → throws ResourceNotFoundException")
        void acceptInvitation_InvitationNotFound() {
            String token = "nonexistent-token";
            String email = "guest@example.com";

            when(invitationRepository.findByTokenAndAcceptedAtIsNull(token)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invitationService.acceptInvitation(token, email))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        @DisplayName("User not found by email → throws ResourceNotFoundException")
        void acceptInvitation_UserNotFound() {
            String token = "valid-token";
            String email = "nonexistent@example.com";
            Invitation invitation = Invitation.builder()
                    .email(email)
                    .tenantId(UUID.randomUUID())
                    .invitedBy(UUID.randomUUID())
                    .expiresAt(ZonedDateTime.now().plusDays(7))
                    .build();

            when(invitationRepository.findByTokenAndAcceptedAtIsNull(token)).thenReturn(Optional.of(invitation));
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invitationService.acceptInvitation(token, email))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User with email");
        }

        @Test
        @DisplayName("Successful acceptance → creates UserTenant and returns response")
        void acceptInvitation_Success() {
            String token = "valid-token";
            String email = "guest@example.com";
            UUID tenantId = UUID.randomUUID();
            UUID inviterId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UserEntity user = UserEntity.builder().id(userId).email(email).build();
            Invitation invitation = Invitation.builder()
                    .email(email)
                    .tenantId(tenantId)
                    .invitedBy(inviterId)
                    .role(RoleName.VIEWER)
                    .expiresAt(ZonedDateTime.now().plusDays(7))
                    .build();
            UserTenant savedUserTenant = UserTenant.builder()
                    .userId(userId)
                    .tenantId(tenantId)
                    .role(RoleName.VIEWER)
                    .build();
            Invitation savedInvitation = Invitation.builder()
                    .email(email)
                    .acceptedAt(ZonedDateTime.now())
                    .build();

            when(invitationRepository.findByTokenAndAcceptedAtIsNull(token)).thenReturn(Optional.of(invitation));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(userTenantRepository.save(any())).thenReturn(savedUserTenant);
            when(invitationRepository.save(any())).thenReturn(savedInvitation);
            when(invitationMapper.toResponse(any(), any(), any())).thenReturn(new InvitationResponse(
                    UUID.randomUUID(),
                    tenantId,
                    "Acme Corp",
                    email,
                    RoleName.VIEWER,
                    "Admin",
                    null,
                    null,
                    true
            ));
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(Tenant.builder().id(tenantId).name("Acme Corp").build()));
            when(userRepository.findById(inviterId)).thenReturn(Optional.of(UserEntity.builder().id(inviterId).name("Admin").build()));

            invitationService.acceptInvitation(token, email);

            verify(userTenantRepository).save(any(UserTenant.class));
            verify(invitationRepository).save(any(Invitation.class));
        }
    }

    @Nested
    @DisplayName("cancelInvitation")
    class CancelInvitationTests {

        @Test
        @DisplayName("Cancel by inviter → Success")
        void cancelInvitation_ByInviter_Success() {
            UUID invitationId = UUID.randomUUID();
            String inviterEmail = "admin@example.com";
            UUID inviterId = UUID.randomUUID();
            UserEntity inviter = UserEntity.builder().id(inviterId).email(inviterEmail).build();
            Invitation invitation = Invitation.builder().id(invitationId).invitedBy(inviterId).build();

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            invitationService.cancelInvitation(invitationId, inviterEmail);

            verify(invitationRepository).delete(invitation);
        }

        @Test
        @DisplayName("Cancel by another Admin → Success")
        void cancelInvitation_ByAnotherAdmin_Success() {
            UUID invitationId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            String adminEmail = "other-admin@example.com";
            UUID adminId = UUID.randomUUID();
            UserEntity admin = UserEntity.builder().id(adminId).email(adminEmail).build();
            
            Invitation invitation = Invitation.builder().id(invitationId).tenantId(tenantId).invitedBy(UUID.randomUUID()).build();
            UserTenant adminTenant = UserTenant.builder().role(RoleName.ADMIN).build();

            when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(admin));
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(userTenantRepository.findByUserIdAndTenantId(adminId, tenantId)).thenReturn(Optional.of(adminTenant));

            invitationService.cancelInvitation(invitationId, adminEmail);

            verify(invitationRepository).delete(invitation);
        }

        @Test
        @DisplayName("User not found by email → throws ResourceNotFoundException")
        void cancelInvitation_UserNotFound() {
            UUID invitationId = UUID.randomUUID();
            String nonexistentEmail = "nonexistent@example.com";

            when(userRepository.findByEmail(nonexistentEmail)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invitationService.cancelInvitation(invitationId, nonexistentEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User with email");
        }

        @Test
        @DisplayName("Invitation not found → throws ResourceNotFoundException")
        void cancelInvitation_InvitationNotFound() {
            String inviterEmail = "admin@example.com";
            UUID inviterId = UUID.randomUUID();
            UserEntity inviter = UserEntity.builder().id(inviterId).email(inviterEmail).build();
            UUID invitationId = UUID.randomUUID();

            when(userRepository.findByEmail(inviterEmail)).thenReturn(Optional.of(inviter));
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invitationService.cancelInvitation(invitationId, inviterEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("does not exist");
        }

        @Test
        @DisplayName("Viewer cannot cancel → throws AuthorizationException")
        void cancelInvitation_NonAdminCannotCancel() {
            UUID invitationId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            String viewerEmail = "viewer@example.com";
            UUID viewerId = UUID.randomUUID();
            UserEntity viewer = UserEntity.builder().id(viewerId).email(viewerEmail).build();
            
            Invitation invitation = Invitation.builder().id(invitationId).tenantId(tenantId).invitedBy(UUID.randomUUID()).build();
            UserTenant viewerTenant = UserTenant.builder().role(RoleName.VIEWER).build();

            when(userRepository.findByEmail(viewerEmail)).thenReturn(Optional.of(viewer));
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(userTenantRepository.findByUserIdAndTenantId(viewerId, tenantId)).thenReturn(Optional.of(viewerTenant));

            assertThatThrownBy(() -> invitationService.cancelInvitation(invitationId, viewerEmail))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        @DisplayName("Non-member cannot cancel → throws AuthorizationException")
        void cancelInvitation_NonMemberCannotCancel() {
            UUID invitationId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            String outsiderEmail = "outsider@example.com";
            UUID outsiderId = UUID.randomUUID();
            UserEntity outsider = UserEntity.builder().id(outsiderId).email(outsiderEmail).build();
            
            Invitation invitation = Invitation.builder().id(invitationId).tenantId(tenantId).invitedBy(UUID.randomUUID()).build();

            when(userRepository.findByEmail(outsiderEmail)).thenReturn(Optional.of(outsider));
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(userTenantRepository.findByUserIdAndTenantId(outsiderId, tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> invitationService.cancelInvitation(invitationId, outsiderEmail))
                    .isInstanceOf(AuthorizationException.class);
        }
    }
}
