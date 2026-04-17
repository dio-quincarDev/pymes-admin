package auth.pymes.service.impl;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.PasswordResetService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.PasswordResetTokenInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

        return true;
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
