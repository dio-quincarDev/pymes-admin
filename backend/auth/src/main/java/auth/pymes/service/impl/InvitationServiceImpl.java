package auth.pymes.service.impl;

import auth.pymes.common.models.dto.request.CreateInvitationRequest;
import auth.pymes.common.models.dto.response.InvitationResponse;
import auth.pymes.common.models.entities.Invitation;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.mappers.InvitationMapper;
import auth.pymes.repositories.InvitationRepository;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.InvitationService;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

import static auth.pymes.utils.exception.CodigoError.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationServiceImpl implements InvitationService {

    private final UserEntityRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRepository userTenantRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationMapper invitationMapper;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public Page<InvitationResponse> getPendingInvitations(Pageable pageable, Object principal) {
        String email = extractEmail(principal);
        Page<Invitation> invitations = invitationRepository.findByEmailAndAcceptedAtIsNull(email, pageable);
        
        return invitations.map(invitation -> {
            Tenant tenant = tenantRepository.findById(invitation.getTenantId()).orElse(null);
            UserEntity inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);
            return invitationMapper.toResponse(invitation, tenant, inviter);
        });
    }

    @Override
    @Transactional
    public InvitationResponse createInvitation(CreateInvitationRequest request, Object principal) {
        String inviterEmail = extractEmail(principal);
        UserEntity inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, inviterEmail));

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException(TENANT_NOT_FOUND, request.tenantId()));

        UserTenant inviterTenant = userTenantRepository.findByUserIdAndTenantId(inviter.getId(), request.tenantId())
                .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT, request.tenantId()));

        if (inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.OWNER && inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.ADMIN) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
        }

        if (!inviterTenant.getRole().hasMorePowerThan(request.role())) {
            throw new AuthorizationException(INSUFFICIENT_PERMISSIONS,
                    "Cannot invite a user with role equal or higher than yours");
        }

        long currentMembers = userTenantRepository.countByTenantIdAndIsActiveTrue(request.tenantId());
        if (currentMembers >= tenant.getMaxUsers()) {
            throw new DuplicateResourceException(MAX_USERS_REACHED, tenant.getMaxUsers());
        }

        if (invitationRepository.existsByTenantIdAndEmailAndAcceptedAtIsNull(request.tenantId(), request.email())) {
            throw new DuplicateResourceException(EMAIL_ALREADY_INVITED, request.email());
        }

        UserEntity existingUser = userRepository.findByEmail(request.email()).orElse(null);
        if (existingUser != null) {
            if (userTenantRepository.findByUserIdAndTenantId(existingUser.getId(), request.tenantId()).isPresent()) {
                throw new DuplicateResourceException(DUPLICATE_RESOURCE, "User is already a member of this tenant");
            }
        }

        String token = UUID.randomUUID().toString();
        Invitation invitation = Invitation.builder()
                .tenantId(request.tenantId())
                .email(request.email())
                .role(request.role())
                .invitedBy(inviter.getId())
                .token(token)
                .expiresAt(ZonedDateTime.now().plusDays(7))
                .build();

        invitation = invitationRepository.save(invitation);
        log.info("Usuario {} invitó a {} al tenant {} con rol {}", 
                inviter.getEmail(), request.email(), tenant.getName(), request.role());

        sendInvitationEmail(inviter, tenant, invitation);

        return invitationMapper.toResponse(invitation, tenant, inviter);
    }

    @Override
    @Transactional
    public InvitationResponse acceptInvitation(String invitationToken, Object principal) {
        String email = extractEmail(principal);

        Invitation invitation = invitationRepository.findByTokenAndAcceptedAtIsNull(invitationToken)
                .orElseThrow(() -> new ResourceNotFoundException(INVITATION_NOT_FOUND, invitationToken));

        if (!invitation.getEmail().equals(email)) {
            throw new AuthorizationException(INVALID_INPUT, "Invitation email does not match authenticated user");
        }

        if (invitation.getExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new InvalidInputException(INVITATION_EXPIRED);
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        UserTenant userTenant = UserTenant.builder()
                .userId(user.getId())
                .tenantId(invitation.getTenantId())
                .role(invitation.getRole())
                .invitedBy(invitation.getInvitedBy())
                .acceptedAt(ZonedDateTime.now())
                .isActive(true)
                .build();

        userTenantRepository.save(userTenant);

        invitation.setAcceptedAt(ZonedDateTime.now());
        invitationRepository.save(invitation);

        log.info("Usuario {} aceptó invitación al tenant {}", user.getEmail(), invitation.getTenantId());

        Tenant tenant = tenantRepository.findById(invitation.getTenantId()).orElse(null);
        UserEntity inviter = userRepository.findById(invitation.getInvitedBy()).orElse(null);

        return invitationMapper.toResponse(invitation, tenant, inviter);
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID invitationId, Object principal) {
        String inviterEmail = extractEmail(principal);
        UserEntity inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, inviterEmail));

        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException(INVITATION_NOT_FOUND, invitationId));

        if (!invitation.getInvitedBy().equals(inviter.getId())) {
            UserTenant inviterTenant = userTenantRepository.findByUserIdAndTenantId(inviter.getId(), invitation.getTenantId())
                    .orElseThrow(() -> new AuthorizationException(USER_NOT_IN_TENANT));
            
            if (inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.OWNER && inviterTenant.getRole() != auth.pymes.common.models.enums.RoleName.ADMIN) {
                throw new AuthorizationException(INSUFFICIENT_PERMISSIONS);
            }
        }

        invitationRepository.delete(invitation);
        log.info("Invitación {} cancelada por {}", invitationId, inviter.getEmail());
    }

    private String extractEmail(Object principal) {
        if (principal instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String email) {
            return email;
        }
        throw new AuthorizationException(UNAUTHORIZED_ACCESS, "Could not extract email from principal");
    }

    private void sendInvitationEmail(UserEntity inviter, Tenant tenant, Invitation invitation) {
        String baseUrl = frontendUrl.replace(":9000", ":9200");
        String acceptUrl = baseUrl + "/#/accept-invitation?token=" + invitation.getToken();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(invitation.getEmail());
            helper.setSubject("Fuiste invitado a unirte a " + tenant.getName() + " en Pymes Admin");

            String htmlContent = buildInvitationEmail(inviter.getName(), tenant.getName(), acceptUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de invitación enviado exitosamente a: {}", invitation.getEmail());
        } catch (MessagingException e) {
            log.error("Error al enviar email de invitación a {}: {}", invitation.getEmail(), e.getMessage());
            // No lanzamos excepción para no romper la transacción del registro si el mail falla
        }
    }

    private String buildInvitationEmail(String inviterName, String tenantName, String acceptUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f9fafb; margin: 0; padding: 20px; color: #111827; }
                    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
                    .header { background: #4F46E5; color: white; padding: 40px 20px; text-align: center; }
                    .content { padding: 40px 30px; line-height: 1.6; }
                    .button-container { text-align: center; margin: 35px 0; }
                    .button { display: inline-block; background-color: #4F46E5; color: #ffffff !important; padding: 16px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 2px 4px rgba(79, 70, 229, 0.3); }
                    .footer { background: #f3f4f6; padding: 24px; text-align: center; color: #6b7280; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin:0; font-size: 28px;">Pymes Admin</h1>
                        <p style="margin:10px 0 0 0; opacity: 0.9;">Gestión inteligente para tu negocio</p>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px;">Hola,</p>
                        <p><strong>%s</strong> te ha invitado a formar parte del equipo de <strong>%s</strong> en Pymes Admin.</p>
                        <p>Al unirte, podrás colaborar en la gestión de la empresa según el rol que te ha sido asignado.</p>
                        
                        <div class="button-container">
                            <a href="%s" class="button">Aceptar Invitación</a>
                        </div>
                        
                        <p>Este enlace de invitación expirará en 7 días.</p>
                        <p>Si no esperabas esta invitación, puedes ignorar este correo.</p>
                    </div>
                    <div class="footer">
                        <p>Pymes Admin &copy; 2026. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(inviterName, tenantName, acceptUrl);
    }
}
