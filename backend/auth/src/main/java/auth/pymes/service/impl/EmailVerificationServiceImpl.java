package auth.pymes.service.impl;

import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.VerifyEmailRequest;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.AuthService;
import auth.pymes.service.EmailVerificationService;
import auth.pymes.service.EmailService;
import auth.pymes.common.config.RateLimitService;
import auth.pymes.utils.exception.CodigoError;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.EmailVerificationTokenInvalidException;
auth.pymes.utils.exception.custom.DuplicateResourceException;
import auth.pymes.utils.exception.custom.InvalidInputException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para gestión de emails y tokens de verificación con aislamiento por tenant en Redis.
 * Cada tenant tiene su propio espacio de nombres de claves.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final String VERIFY_KEY_PREFIX = "email:verify:";
    private static final String PENDING_REG_PREFIX = "temp-register:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(15);
    private final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserEntityRepository userRepository;
    private final EmailService emailService;
    private final AuthService authService;
    private final RateLimitService rateLimitService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailVerificationServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            UserEntityRepository userRepository,
            EmailService emailService,
            @Lazy AuthService authService,
            RateLimitService rateLimitService) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public void generateAndSendPendingRegistrationEmail(RegisterRequest request, String tenantId) {
        String token = generateSecureToken();
        String key = PENDING_REG_PREFIX + token;

        // Guardar el DTO completo en Redis con prefix de tenant
        redisTemplate.opsForValue().set("tenant:" + tenantId + ":" + key, request, TOKEN_TTL.toMillis(), TimeUnit.MILLISECONDS);
        // Índice por email para detección rápida de duplicados, también con prefix de tenant
        redisTemplate.opsForValue().set("tenant:" + tenantId + ":" + PENDING_REG_PREFIX + "email:" + request.email(), token, TOKEN_TTL.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Registro pendiente guardado en Redis para tenant {} y email: {}", tenantId, request.email());

        sendVerificationEmail(request.name(), request.email(), token);
    }

    @Override
    public boolean existsPendingRegistration(String email, String tenantId) {
        String key = "tenant:" + tenantId + ":" + PENDING_REG_PREFIX + "email:" + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public String generateVerificationToken(UserEntity user, String tenantId) {
        String token = generateSecureToken();
        String key = VERIFY_KEY_PREFIX + token;

        redisTemplate.opsForValue().set("tenant:" + tenantId + ":" + key, user.getEmail(), TOKEN_TTL.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Token de verificación generado para usuario: {} (TTL: {} min) en tenant {}", user.getEmail(), TOKEN_TTL.toMinutes(), tenantId);

        return token;
    }

    @Override
    @Transactional
    public auth.pymes.common.models.dto.response.AuthResponse verifyEmail(VerifyEmailRequest request, String tenantId, HttpServletRequest httpRequest) {
        String token = request.token();
        String email = request.email();

        // Validar tamaño del token para evitar claves Redis gigantes / DoS
        if (token == null || token.isBlank() || token.length() > 128) {
            throw new EmailVerificationTokenInvalidException();
        }

        // 1. Intentar flujo PENDING REGISTRATION (con tenant isolation)
        String pendingKey = "tenant:" + tenantId + ":" + PENDING_REG_PREFIX + token;
        Object pendingData = redisTemplate.opsForValue().get(pendingKey);

        if (pendingData instanceof RegisterRequest regRequest) {
            // Validar que el email coincida (SECURITY FIX)
            if (!regRequest.email().equalsIgnoreCase(email)) {
                log.warn("Email mismatch en pending registration tenant {}: expected={}, actual={}", tenantId, regRequest.email(), email);
                throw new EmailVerificationTokenInvalidException();
            }

            // Completar registro y login automático
            auth.pymes.common.models.dto.response.AuthResponse response = authService.completeRegistration(regRequest, httpRequest);
            redisTemplate.delete(pendingKey);
            redisTemplate.delete("tenant:" + tenantId + ":" + PENDING_REG_PREFIX + "email:" + regRequest.email());
            log.info("Registro completado y email verificado vía Redis para tenant {} y email: {}", tenantId, email);
            return response;
        }

        // 2. Intentar flujo LEGACY (Usuario ya en DB), también con tenant isolation
        String verifyKey = "tenant:" + tenantId + ":" + VERIFY_KEY_PREFIX + token;
        Object storedEmail = redisTemplate.opsForValue().get(verifyKey);

        if (storedEmail == null) {
            throw new EmailVerificationTokenInvalidException();
        }

        String emailInRedis = storedEmail.toString();
        // Validar que el email coincida (SECURITY FIX)
        if (!emailInRedis.equalsIgnoreCase(email)) {
            log.warn("Email mismatch en legacy verification tenant {}: expected={}, actual={}", tenantId, emailInRedis, email);
            throw new EmailVerificationTokenInvalidException();
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(CodigoError.USER_NOT_FOUND_BY_EMAIL, email));

        if (user.isEmailVerified()) {
            throw new DuplicateResourceException(CodigoError.EMAIL_ALREADY_VERIFIED);
        }

        user.markEmailAsVerified();
        userRepository.save(user);

        redisTemplate.delete(verifyKey);
        log.info("Email verificado exitosamente (Legacy) para tenant {} y usuario: {}", tenantId, email);

        // Para usuarios legacy, no tenemos el password para el login automático inmediato vía completeRegistration,
        // pero podemos generar tokens si el flujo lo permite o simplemente retornar tokens si el usuario es válido.
        // Como este es el paso final de verificación, generamos tokens para login automático.
        // NOTA: Para usuarios legacy necesitamos el tenant activo.
        // Por simplicidad en este MVP, si es legacy, pedimos que haga login manual o buscamos su primer tenant.
        return generateAuthResponseForLegacyUser(user, httpRequest);
    }

    private auth.pymes.common.models.dto.response.AuthResponse generateAuthResponseForLegacyUser(UserEntity user, HttpServletRequest httpRequest) {
        // Implementación simplificada: buscar primer tenant o lanzar excepción pidiendo login
        // En este proyecto, el flujo normal será PENDING REGISTRATION de ahora en adelante.
        log.info("Usuario legacy verificado. Se requiere login manual para obtener tokens de sesión.");
        return new auth.pymes.common.models.dto.response.AuthResponse(null, null, null, null);
    }

    @Override
    @Transactional
    public String resendVerificationToken(String email, String tenantId) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException(CodigoError.USER_NOT_FOUND_BY_EMAIL, email));

        if (user.isEmailVerified()) {
            throw new DuplicateResourceException(CodigoError.EMAIL_ALREADY_VERIFIED);
        }

        String token = generateVerificationToken(user, tenantId);
        sendVerificationEmail(user.getName(), user.getEmail(), token);
        return token;
    }

    @Override
    @Transactional
    public void createAndSendVerificationEmail(UserEntity user, String tenantId) {
        String token = generateVerificationToken(user, tenantId);
        sendVerificationEmail(user.getName(), user.getEmail(), token);
        log.info("Email de verificación enviado a: {} para tenant {}", user.getEmail(), tenantId);
    }

    private void sendVerificationEmail(String name, String email, String token) {
        // Usamos el puerto 9200 para PWA y el prefijo /#/ para hash routing de Quasar
        String baseUrl = frontendUrl.replace(":9000", ":9200");
        String verifyUrl = baseUrl + "/#/verify?token=" + token + "&email=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);

        Map<String, Object> variables = Map.of(
                "name", name,
                "url", verifyUrl
        );

        emailService.send(email, "Verifica tu cuenta en Pymes Admin", "verification", variables);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}