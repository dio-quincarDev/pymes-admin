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

        generateVerificationToken(user);
        sendVerificationEmail(user);
        return null;
    }

    @Override
    @Transactional
    public void createAndSendVerificationEmail(UserEntity user) {
        String token = generateVerificationToken(user);
        sendVerificationEmail(user);
        log.info("Email de verificación enviado a: {}", user.getEmail());
    }

    private void sendVerificationEmail(UserEntity user) {
        String verifyUrl = frontendUrl + "/verify?token=" + generateSecureTokenForEmail(user);

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
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    .header { background: #4F46E5; color: white; padding: 30px; text-align: center; }
                    .content { padding: 30px; }
                    .button { display: inline-block; background: #4F46E5; color: white; padding: 14px 32px; text-decoration: none; border-radius: 6px; font-weight: bold; margin: 20px 0; }
                    .footer { background: #f8f8f8; padding: 20px; text-align: center; color: #888; font-size: 12px; }
                    .link-text { word-break: break-all; color: #4F46E5; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Pymes Admin</h1>
                        <p>Verifica tu cuenta</p>
                    </div>
                    <div class="content">
                        <p>Hola <strong>%s</strong>,</p>
                        <p>Gracias por registrarte en Pymes Admin. Para completar tu registro, haz clic en el botón de abajo:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Verificar mi cuenta</a>
                        </p>
                        <p>O copia y pega el siguiente enlace en tu navegador:</p>
                        <p class="link-text">%s</p>
                        <p>Este enlace expirará en 15 minutos.</p>
                        <p>Si no creaste esta cuenta, puedes ignorar este email.</p>
                    </div>
                    <div class="footer">
                        <p>Pymes Admin &copy; 2026. Todos los derechos reservados.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, verifyUrl, verifyUrl);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
