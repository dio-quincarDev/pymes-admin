package auth.pymes.service.impl;

import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.EmailVerificationService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.EmailVerificationTokenInvalidException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Override
    @Transactional
    public String generateVerificationToken(auth.pymes.common.models.entities.UserEntity user) {
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
        auth.pymes.common.models.entities.UserEntity user = userRepository.findByEmail(email)
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
        auth.pymes.common.models.entities.UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(CodigoError.USER_NOT_FOUND_BY_EMAIL, email));

        if (user.isEmailVerified()) {
            throw new DuplicateResourceException(CodigoError.EMAIL_ALREADY_VERIFIED);
        }

        return generateVerificationToken(user);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
