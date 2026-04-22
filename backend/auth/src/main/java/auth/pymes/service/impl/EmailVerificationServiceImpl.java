package auth.pymes.service.impl;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.EmailVerificationService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.EmailVerificationTokenInvalidException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final String VERIFY_KEY_PREFIX = "email:verify:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserEntityRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Transactional
    public String generateVerificationToken(UserEntity user) {
        String token = generateSecureToken();
        String key = VERIFY_KEY_PREFIX + token;

        redisTemplate.opsForValue().set(key, user.getEmail(), TOKEN_TTL);
        log.info("Token de verificación generado para usuario: {} (TTL: {} min)", user.getEmail(), TOKEN_TTL.toMinutes());

        return token;
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        String key = VERIFY_KEY_PREFIX + token;
        Object emailObj = redisTemplate.opsForValue().get(key);

        if (emailObj == null) {
            throw new EmailVerificationTokenInvalidException();
        }

        String email = emailObj.toString();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(CodigoError.USER_NOT_FOUND_BY_EMAIL, email));

        if (user.isEmailVerified()) {
            throw new DuplicateResourceException(CodigoError.EMAIL_ALREADY_VERIFIED);
        }

        user.markEmailAsVerified();
        userRepository.save(user);

        redisTemplate.delete(key);
        log.info("Email verificado exitosamente para usuario: {}", email);
    }

    @Override
    @Transactional
    public String resendVerificationToken(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(CodigoError.USER_NOT_FOUND_BY_EMAIL, email));

        if (user.isEmailVerified()) {
            throw new DuplicateResourceException(CodigoError.EMAIL_ALREADY_VERIFIED);
        }

        String token = generateVerificationToken(user);
        sendVerificationEmail(user, token);
        return token;
    }

    @Override
    @Transactional
    public void createAndSendVerificationEmail(UserEntity user) {
        String token = generateVerificationToken(user);
        sendVerificationEmail(user, token);
        log.info("Email de verificación enviado a: {}", user.getEmail());
    }

    private void sendVerificationEmail(UserEntity user, String token) {
        // Usamos el puerto 9200 para PWA y el prefijo /#/ para hash routing de Quasar
        String baseUrl = frontendUrl.replace(":9000", ":9200");
        String verifyUrl = baseUrl + "/#/verify?token=" + token + "&email=" + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("Verifica tu cuenta en Pymes Admin");

            String htmlContent = buildVerificationEmail(user.getName(), verifyUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de verificación enviado exitosamente a: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Error al enviar email de verificación a {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Error al enviar email de verificación", e);
        }
    }

    /**
     * Generates a token and stores it in Redis for email verification.
     * Used when sending the verification email.
     */
    private String generateSecureTokenForEmail(UserEntity user) {
        String token = generateSecureToken();
        String key = VERIFY_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, user.getEmail(), TOKEN_TTL);
        return token;
    }

    private String buildVerificationEmail(String firstName, String verifyUrl) {
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
                    .warning { font-size: 13px; color: #9ca3af; margin-top: 25px; border-top: 1px solid #e5e7eb; padding-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin:0; font-size: 28px;">Pymes Admin</h1>
                        <p style="margin:10px 0 0 0; opacity: 0.9;">Gestión inteligente para tu negocio</p>
                    </div>
                    <div class="content">
                        <p style="font-size: 18px;">Hola <strong>%s</strong>,</p>
                        <p>Gracias por unirte a Pymes Admin. Estamos emocionados de tenerte a bordo. Para comenzar a utilizar todas las funciones de la plataforma, por favor confirma tu dirección de correo electrónico:</p>
                        
                        <div class="button-container">
                            <a href="%s" class="button">Verificar mi cuenta</a>
                        </div>
                        
                        <p>Este enlace de seguridad expirará en <strong>15 minutos</strong>.</p>
                        
                        <div class="warning">
                            <p>Si no has creado una cuenta en nuestra plataforma, puedes ignorar este mensaje de forma segura.</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>Pymes Admin &copy; 2026. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, verifyUrl);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
