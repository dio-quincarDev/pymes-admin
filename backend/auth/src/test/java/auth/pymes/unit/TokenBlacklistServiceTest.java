package auth.pymes.unit;

import auth.pymes.service.impl.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void revokeToken_UsesMillisecondsNotSeconds() {
        long expirationMs = 3600000L; // 1 hour in millis
        String token = "test-token";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.revokeToken(token, expirationMs);

        ArgumentCaptor<TimeUnit> timeUnitCaptor = ArgumentCaptor.forClass(TimeUnit.class);
        verify(valueOperations).set(any(), any(), eq(expirationMs), timeUnitCaptor.capture());

        assertThat(timeUnitCaptor.getValue()).isEqualTo(TimeUnit.MILLISECONDS);
    }

    @Test
    void isTokenRevoked_WhenKeyExists_ReturnsTrue() {
        String token = "revoked-token";
        when(redisTemplate.hasKey("auth:token_blacklist:" + token)).thenReturn(true);

        assertThat(tokenBlacklistService.isTokenRevoked(token)).isTrue();
    }

    @Test
    void isTokenRevoked_WhenKeyMissing_ReturnsFalse() {
        String token = "valid-token";
        when(redisTemplate.hasKey("auth:token_blacklist:" + token)).thenReturn(false);

        assertThat(tokenBlacklistService.isTokenRevoked(token)).isFalse();
    }
}
