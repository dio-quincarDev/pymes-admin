package auth.pymes.unit;

import auth.pymes.common.models.dto.request.RegisterRequest;
import auth.pymes.common.models.dto.request.VerifyEmailRequest;
import auth.pymes.common.models.dto.response.AuthResponse;
import auth.pymes.service.AuthService;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.EmailService;
import auth.pymes.service.impl.EmailVerificationServiceImpl;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.EmailVerificationTokenInvalidException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.enums.AuthProvider;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    private UserEntity unverifiedUser;
    private UserEntity verifiedUser;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationServiceImpl(redisTemplate, userRepository, emailService, authService);
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:9200");

        unverifiedUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.LOCAL)
                .providerId("test@example.com")
                .isActive(true)
                .emailVerifiedAt(null)
                .build();

        verifiedUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("verified@example.com")
                .name("Verified User")
                .provider(AuthProvider.LOCAL)
                .providerId("verified@example.com")
                .isActive(true)
                .emailVerifiedAt(ZonedDateTime.now().minusHours(1))
                .build();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== generateAndSendPendingRegistrationEmail ====================

    @Test
    void generateAndSendPendingRegistrationEmail_StoresRequestInRedisAndSendsEmail() {
        RegisterRequest request = new RegisterRequest("New User", "new@example.com", "password", "New Company", "new-company");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        emailVerificationService.generateAndSendPendingRegistrationEmail(request);

        verify(valueOperations).set(startsWith("temp-register:"), eq(request), any());
        verify(emailService).send(eq("new@example.com"), eq("Verifica tu cuenta en Pymes Admin"), eq("verification"), any());
    }

    // ==================== generateVerificationToken ====================

    @Test
    void generateVerificationToken_ReturnsTokenAndStoresInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = emailVerificationService.generateVerificationToken(unverifiedUser);

        assertThat(token).isNotBlank().hasSize(64);

        verify(valueOperations).set(eq("email:verify:" + token), eq("test@example.com"), any());
    }

    // ==================== verifyEmail ====================

    @Test
    void verifyEmail_WithPendingRegistration_MarksUserAsVerifiedAndCompletesLogin() {
        String token = "pending-token";
        RegisterRequest regRequest = new RegisterRequest("Test User", "test@example.com", "password", "Company", "company");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        AuthResponse authResponse = new AuthResponse("access", "refresh", null, null);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("temp-register:" + token)).thenReturn(regRequest);
        when(authService.completeRegistration(any(), any())).thenReturn(authResponse);

        AuthResponse result = emailVerificationService.verifyEmail(new VerifyEmailRequest(token, "test@example.com"), httpRequest);

        assertThat(result.accessToken()).isEqualTo("access");
        verify(redisTemplate).delete("temp-register:" + token);
    }

    @Test
    void verifyEmail_WithPendingRegistration_EmailMismatch_ThrowsException() {
        String token = "pending-token";
        RegisterRequest regRequest = new RegisterRequest("Test User", "test@example.com", "password", "Company", "company");
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("temp-register:" + token)).thenReturn(regRequest);

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(new VerifyEmailRequest(token, "wrong@example.com"), httpRequest))
                .isInstanceOf(EmailVerificationTokenInvalidException.class);
    }

    @Test
    void verifyEmail_WithLegacyToken_MarksUserAsVerified() {
        String token = "legacy-token";
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("temp-register:" + token)).thenReturn(null);
        when(valueOperations.get("email:verify:" + token)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(unverifiedUser);

        AuthResponse result = emailVerificationService.verifyEmail(new VerifyEmailRequest(token, "test@example.com"), httpRequest);

        assertThat(unverifiedUser.isEmailVerified()).isTrue();
        assertThat(unverifiedUser.getEmailVerifiedAt()).isNotNull();
        verify(redisTemplate).delete("email:verify:" + token);
    }

    @Test
    void verifyEmail_WithInvalidToken_ThrowsEmailVerificationTokenInvalidException() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("temp-register:" + "invalid")).thenReturn(null);
        when(valueOperations.get("email:verify:" + "invalid")).thenReturn(null);

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(new VerifyEmailRequest("invalid", "test@example.com"), httpRequest))
                .isInstanceOf(EmailVerificationTokenInvalidException.class);
    }

    @Test
    void verifyEmail_WithAlreadyVerifiedUser_ThrowsDuplicateResourceException() {
        String token = "legacy-token";
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("temp-register:" + token)).thenReturn(null);
        when(valueOperations.get("email:verify:" + token)).thenReturn("verified@example.com");
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(new VerifyEmailRequest(token, "verified@example.com"), httpRequest))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void verifyEmail_UserNotFoundInDB_ThrowsAuthenticationException() {
        String token = "orphan-token";
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("temp-register:" + token)).thenReturn(null);
        when(valueOperations.get("email:verify:" + token)).thenReturn("deleted@example.com");
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(new VerifyEmailRequest(token, "deleted@example.com"), httpRequest))
                .isInstanceOf(AuthenticationException.class);
    }

    // ==================== resendVerificationToken ====================

    @Test
    void resendVerificationToken_GeneratesNewTokenForUnverifiedUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String token = emailVerificationService.resendVerificationToken("test@example.com");

        assertThat(token).isNotBlank().hasSize(64);
        verify(valueOperations).set(startsWith("email:verify:"), eq("test@example.com"), any());
    }

    @Test
    void resendVerificationToken_ForVerifiedUser_ThrowsDuplicateResourceException() {
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

        assertThatThrownBy(() -> emailVerificationService.resendVerificationToken("verified@example.com"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void resendVerificationToken_ForNonExistentUser_ThrowsAuthenticationException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.resendVerificationToken("nonexistent@example.com"))
                .isInstanceOf(AuthenticationException.class);
    }

    // ==================== createAndSendVerificationEmail ====================

    @Test
    void createAndSendVerificationEmail_SendsEmailToUnverifiedUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        emailVerificationService.createAndSendVerificationEmail(unverifiedUser);

        verify(valueOperations).set(startsWith("email:verify:"), eq("test@example.com"), any());
        verify(emailService).send(eq("test@example.com"), eq("Verifica tu cuenta en Pymes Admin"), eq("verification"), any());
    }
}
