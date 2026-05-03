package auth.pymes.service.impl;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.EmailVerificationService;
import auth.pymes.service.EmailService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.EmailVerificationTokenInvalidException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final String VERIFY_KEY_PREFIX = "email:verify:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserEntityRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

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

        Map<String, Object> variables = Map.of(
                "name", user.getName(),
                "url", verifyUrl
        );

        emailService.send(user.getEmail(), "Verifica tu cuenta en Pymes Admin", "verification", variables);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
