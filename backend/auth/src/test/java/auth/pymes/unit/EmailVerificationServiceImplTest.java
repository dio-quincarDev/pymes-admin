package auth.pymes.unit;

import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.EmailVerificationService;
import auth.pymes.service.impl.EmailVerificationServiceImpl;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.AuthorizationException;
import auth.pymes.utils.exception.auth.EmailVerificationTokenInvalidException;
import auth.pymes.utils.exception.custom.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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

    @InjectMocks
    private EmailVerificationServiceImpl emailVerificationService;

    private UserEntity unverifiedUser;
    private UserEntity verifiedUser;

    @BeforeEach
    void setUp() {
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
    void verifyEmail_WithValidToken_MarksUserAsVerified() {
        String token = "valid-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verify:" + token)).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(userRepository.save(any(UserEntity.class))).thenReturn(unverifiedUser);

        emailVerificationService.verifyEmail(token);

        assertThat(unverifiedUser.isEmailVerified()).isTrue();
        assertThat(unverifiedUser.getEmailVerifiedAt()).isNotNull();

        verify(redisTemplate).delete("email:verify:" + token);
        verify(userRepository).save(unverifiedUser);
    }

    @Test
    void verifyEmail_WithInvalidToken_ThrowsEmailVerificationTokenInvalidException() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verify:invalid")).thenReturn(null);

        assertThatThrownBy(() -> emailVerificationService.verifyEmail("invalid"))
                .isInstanceOf(EmailVerificationTokenInvalidException.class);
    }

    @Test
    void verifyEmail_WithAlreadyVerifiedUser_ThrowsDuplicateResourceException() {
        String token = "valid-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verify:" + token)).thenReturn("verified@example.com");
        when(userRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(token))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void verifyEmail_UserNotFoundInDB_ThrowsAuthenticationException() {
        String token = "orphan-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verify:" + token)).thenReturn("deleted@example.com");
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyEmail(token))
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
}
