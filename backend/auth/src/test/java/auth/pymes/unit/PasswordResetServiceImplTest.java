package auth.pymes.unit;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.enums.AuthProvider;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.PasswordResetService;
import auth.pymes.service.impl.PasswordResetServiceImpl;
import auth.pymes.utils.exception.auth.AuthenticationException;
import auth.pymes.utils.exception.auth.PasswordResetTokenInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService Unit Tests")
class PasswordResetServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .name("Test User")
                .provider(AuthProvider.LOCAL)
                .providerId("user@example.com")
                .isActive(true)
                .password("oldHashedPassword")
                .build();
    }

    // ==================== generateResetToken ====================

    @Nested
    @DisplayName("generateResetToken")
    class GenerateResetTokenTests {

        @Test
        @DisplayName("Existing email → returns true and stores token in Redis")
        void generateResetToken_ExistingEmail_ReturnsTrue() {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            boolean result = passwordResetService.generateResetToken("user@example.com");

            assertThat(result).isTrue();

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).set(keyCaptor.capture(), emailCaptor.capture(), any());

            assertThat(keyCaptor.getValue()).startsWith("password:reset:");
            assertThat(keyCaptor.getValue()).hasSize("password:reset:".length() + 64); // 32 bytes hex = 64 chars
            assertThat(emailCaptor.getValue()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("Non-existent email → returns false (timing attack prevention)")
        void generateResetToken_NonExistentEmail_ReturnsFalse() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            boolean result = passwordResetService.generateResetToken("nonexistent@example.com");

            assertThat(result).isFalse();
            verifyNoInteractions(redisTemplate);
        }
    }

    // ==================== resetPassword ====================

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("Valid token → updates password and deletes token from Redis")
        void resetPassword_ValidToken_UpdatesPassword() {
            String token = "valid-reset-token";
            String newPassword = "NewSecurePass123!";
            String encodedPassword = "newHashedPassword";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password:reset:" + token)).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

            passwordResetService.resetPassword(token, newPassword);

            // Verify password was encoded and set
            verify(passwordEncoder).encode(newPassword);
            assertThat(testUser.getPassword()).isEqualTo(encodedPassword);

            // Verify token was deleted from Redis
            verify(redisTemplate).delete("password:reset:" + token);

            // Verify user was saved
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Invalid token → throws PasswordResetTokenInvalidException")
        void resetPassword_InvalidToken_ThrowsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password:reset:invalid")).thenReturn(null);

            assertThatThrownBy(() -> passwordResetService.resetPassword("invalid", "NewPass123!"))
                    .isInstanceOf(PasswordResetTokenInvalidException.class);

            verifyNoInteractions(userRepository);
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("User not found in DB → throws AuthenticationException")
        void resetPassword_UserNotFound_ThrowsException() {
            String token = "orphan-token";
            String deletedEmail = "deleted@example.com";

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("password:reset:" + token)).thenReturn(deletedEmail);
            when(userRepository.findByEmail(deletedEmail)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword(token, "NewPass123!"))
                    .isInstanceOf(AuthenticationException.class);

            verify(passwordEncoder, never()).encode(any());
        }
    }
}
