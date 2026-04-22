package auth.pymes.service.impl;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.PasswordResetService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.PasswordResetTokenInvalidException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String RESET_KEY_PREFIX = "password:reset:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserEntityRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Transactional
    public boolean generateResetToken(String email) {
        UserEntity user = userRepository.findByEmail(email).orElse(null);

        // Timing attack prevention: siempre toma el mismo tiempo, aunque el email no exista
        if (user == null) {
            log.warn("Solicitud de reset para email no existente: {}", email);
            return false;
        }

        String token = generateSecureToken();
        String key = RESET_KEY_PREFIX + token;

        redisTemplate.opsForValue().set(key, user.getEmail(), TOKEN_TTL);
        log.info("Token de reset generado para usuario: {} (TTL: {} min)", user.getEmail(), TOKEN_TTL.toMinutes());

        sendResetEmail(user, token);

        return true;
    }

    private void sendResetEmail(UserEntity user, String token) {
        String baseUrl = frontendUrl.replace(":9000", ":9200");
        String resetUrl = baseUrl + "/#/reset-password?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Recupera tu contraseña - Pymes Admin");

            String htmlContent = buildResetEmail(user.getName(), resetUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de recuperación de contraseña enviado a: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Error al enviar email de reset a {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Error al enviar email de recuperación", e);
        }
    }

    private String buildResetEmail(String firstName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f9fafb; margin: 0; padding: 20px; color: #111827; }
                    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
                    .header { background: #EF4444; color: white; padding: 40px 20px; text-align: center; }
                    .content { padding: 40px 30px; line-height: 1.6; }
                    .button-container { text-align: center; margin: 35px 0; }
                    .button { display: inline-block; background-color: #EF4444; color: #ffffff !important; padding: 16px 32px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3); }
                    .footer { background: #f3f4f6; padding: 24px; text-align: center; color: #6b7280; font-size: 14px; }
                    .warning { font-size: 13px; color: #9ca3af; margin-top: 25px; border-top: 1px solid #e5e7eb; padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin:0; font-size: 28px;">Pymes Admin</h1>
                        <p style="margin:10px 0 0 0; opacity: 0.9;">Recuperación de cuenta</p>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px;">Hola <strong>%s</strong>,</p>
                        <p>Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en Pymes Admin. Si has sido tú, puedes hacerlo haciendo clic en el siguiente botón:</p>
                        
                        <div class="button-container">
                            <a href="%s" class="button">Restablecer mi contraseña</a>
                        </div>
                        
                        <p>Este enlace de seguridad es de un solo uso y expirará en <strong>15 minutos</strong>.</p>
                        
                        <div class="warning">
                            <p>Si no has solicitado este cambio, por favor ignora este correo o contacta con soporte si tienes dudas. Tu contraseña seguirá siendo la misma.</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>Pymes Admin &copy; 2026. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, resetUrl);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String key = RESET_KEY_PREFIX + token;
        Object emailObj = redisTemplate.opsForValue().get(key);

        if (emailObj == null) {
            throw new PasswordResetTokenInvalidException();
        }

        String email = emailObj.toString();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(CodigoError.USER_NOT_FOUND_BY_EMAIL, email));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redisTemplate.delete(key);
        log.info("Contraseña actualizada exitosamente para usuario: {}", email);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
