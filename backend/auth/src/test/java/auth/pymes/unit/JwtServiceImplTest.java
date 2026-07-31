package auth.pymes.unit;

import auth.pymes.common.models.entities.RefreshToken;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.RefreshTokenRepository;
import auth.pymes.service.JwtService;
import auth.pymes.service.impl.JwtServiceImpl;
import auth.pymes.service.impl.TokenBlacklistService;
import auth.pymes.utils.exception.token.TokenExpiredException;
import auth.pymes.utils.exception.token.TokenInvalidException;
import auth.pymes.utils.exception.token.TokenRevokedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION = 3600000L;
    private static final long REFRESH_EXPIRATION = 86400000L;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private org.springframework.transaction.support.TransactionTemplate txTemplate;

    @InjectMocks
    private JwtServiceImpl jwtService;

    private UserEntity defaultUser;
    private UUID defaultTenantId;
    private String defaultRole;
    private String defaultPlan;

    @BeforeEach
    void setUp() {
        // Inyectar valores de @Value usando la constante
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_EXPIRATION);
        jwtService.init();

        // Datos base
        defaultUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();
        defaultTenantId = UUID.randomUUID();
        defaultRole = "ADMIN";
        defaultPlan = "PREMIUM";
    }

    @Test
    void generateAccessToken_WithValidData_ReturnsValidJwtToken() {
        // Act
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();

        // Verificar claims usando la misma llave del test
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKeyFromSecret(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(defaultUser.getEmail());
        assertThat(claims.get("userId", String.class)).isEqualTo(defaultUser.getId().toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(defaultTenantId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo(defaultRole);
        assertThat(claims.get("plan", String.class)).isEqualTo(defaultPlan);
    }

    private SecretKey getSigningKeyFromSecret(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void generateRefreshToken_WithValidUser_ReturnsValidJwtToken() {
        // Act
        String token = jwtService.generateRefreshToken(defaultUser);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKeyFromSecret(TEST_SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo(defaultUser.getEmail());
        assertThat(claims.get("userId", String.class)).isEqualTo(defaultUser.getId().toString());
    }

    @Test
    void extractUserId_WithValidToken_ReturnsUserId() {
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        assertThat(jwtService.extractUserId(token)).isEqualTo(defaultUser.getId());
    }

    @Test
    void extractTenantId_WithValidToken_ReturnsTenantId() {
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        assertThat(jwtService.extractTenantId(token)).isEqualTo(defaultTenantId);
    }

    @Test
    void extractTenantId_WhenTenantIdIsNull_ReturnsNull() {
        String token = jwtService.generateAccessToken(defaultUser, null, defaultRole, defaultPlan);
        assertThat(jwtService.extractTenantId(token)).isNull();
    }

    @Test
    void extractEmail_WithValidToken_ReturnsEmail() {
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        assertThat(jwtService.extractEmail(token)).isEqualTo(defaultUser.getEmail());
    }

    @Test
    void extractRole_WithValidToken_ReturnsRole() {
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        assertThat(jwtService.extractRole(token)).isEqualTo(defaultRole);
    }

    @Test
    void extractPlan_WithValidToken_ReturnsPlan() {
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        assertThat(jwtService.extractPlan(token)).isEqualTo(defaultPlan);
    }

    @Test
    void extractPlan_WhenPlanIsNull_ReturnsFREE() {
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, null);
        assertThat(jwtService.extractPlan(token)).isEqualTo("FREE");
    }

    @Test
    void isTokenValid_WithValidNonRevokedToken_ReturnsTrue() {
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        when(tokenBlacklistService.isTokenRevoked(token)).thenReturn(false);

        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isTrue();
        verify(tokenBlacklistService).isTokenRevoked(token);
    }

    @Test
    void revokeToken_CallsBlacklistServiceWithCorrectParams() {
        String token = "some-token";
        jwtService.revokeToken(token);
        verify(tokenBlacklistService).revokeToken(token, ACCESS_EXPIRATION);
    }

    @Test
    void isTokenRevoked_DelegatesToBlacklistService() {
        String token = "some-token";
        when(tokenBlacklistService.isTokenRevoked(token)).thenReturn(true);
        assertThat(jwtService.isTokenRevoked(token)).isTrue();
    }

    @Test
    void isTokenValid_WithExpiredToken_ReturnsFalse() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);
        String expiredToken = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_EXPIRATION);

        // Act & Assert
        // Eliminado stubbing de blacklist porque el código fallará antes por expiración
        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void isTokenValid_WithMalformedToken_ReturnsFalse() {
        // Act & Assert
        // Eliminado stubbing de blacklist porque el código fallará por formato
        assertThat(jwtService.isTokenValid("not.a.jwt.token")).isFalse();
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void isTokenValid_WithInvalidSignature_ReturnsFalse() {
        // Arrange: Generar un token con una llave secreta distinta
        String differentSecret = "different_secret_key_that_is_long_enough_to_be_secure_123456";
        SecretKey otherKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));
        
        String invalidSignedToken = Jwts.builder()
                .subject(defaultUser.getEmail())
                .signWith(otherKey)
                .compact();

        // Act & Assert
        assertThat(jwtService.isTokenValid(invalidSignedToken)).isFalse();
        verifyNoInteractions(tokenBlacklistService);
    }

    @Test
    void extractUserId_WhenUserIdClaimMissing_ReturnsNull() {
        // Arrange
        SecretKey key = getSigningKeyFromSecret(TEST_SECRET);
        String tokenWithoutUserId = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(key)
                .compact();

        // Act & Assert
        assertThat(jwtService.extractUserId(tokenWithoutUserId)).isNull();
    }

    // ==================== validateToken Tests ====================

    @Test
    void validateToken_WithValidToken_ReturnsValidatedToken() {
        // Arrange
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        when(tokenBlacklistService.isTokenRevoked(token)).thenReturn(false);

        // Act
        JwtService.ValidatedToken result = jwtService.validateToken(token);

        // Assert
        assertThat(result.userId()).isEqualTo(defaultUser.getId());
        assertThat(result.tenantId()).isEqualTo(defaultTenantId);
        assertThat(result.role()).isEqualTo(defaultRole);
        assertThat(result.email()).isEqualTo(defaultUser.getEmail());
    }

    @Test
    void validateToken_WithExpiredToken_ThrowsTokenExpiredException() {
        // Arrange
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);
        String expiredToken = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_EXPIRATION);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.validateToken(expiredToken))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void validateToken_WithMalformedToken_ThrowsTokenInvalidException() {
        // Act & Assert
        assertThatThrownBy(() -> jwtService.validateToken("not.a.jwt"))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void validateToken_WithInvalidSignature_ThrowsTokenInvalidException() {
        // Arrange
        String differentSecret = "different_secret_key_that_is_long_enough_to_be_secure_123456";
        SecretKey otherKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String invalidSignedToken = Jwts.builder()
                .subject(defaultUser.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(otherKey)
                .compact();

        // Act & Assert
        assertThatThrownBy(() -> jwtService.validateToken(invalidSignedToken))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void validateToken_WithRevokedToken_ThrowsTokenRevokedException() {
        // Arrange
        String token = jwtService.generateAccessToken(defaultUser, defaultTenantId, defaultRole, defaultPlan);
        when(tokenBlacklistService.isTokenRevoked(token)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> jwtService.validateToken(token))
                .isInstanceOf(TokenRevokedException.class);
    }

    @Test
    void validateToken_WithNullTenantId_ReturnsNullTenantId() {
        // Arrange
        String token = jwtService.generateAccessToken(defaultUser, null, defaultRole, defaultPlan);
        when(tokenBlacklistService.isTokenRevoked(token)).thenReturn(false);

        // Act
        JwtService.ValidatedToken result = jwtService.validateToken(token);

        // Assert
        assertThat(result.tenantId()).isNull();
    }

    // ==================== Refresh Token Rotation Tests ====================

    @Test
    void validateAndRevokeRefreshToken_WithValidToken_ReturnsValidationAndRevokes() {
        // Arrange
        String refreshToken = jwtService.generateRefreshToken(defaultUser);
        String tokenHash = jwtService.hashToken(refreshToken);
        RefreshToken entity = RefreshToken.builder()
                .userId(defaultUser.getId())
                .tenantId(defaultTenantId)
                .tokenHash(tokenHash)
                .revoked(false)
                .build();

        when(tokenBlacklistService.isTokenRevoked(refreshToken)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(entity));

        // Act
        JwtService.RefreshTokenValidation result = jwtService.validateAndRevokeRefreshToken(refreshToken);

        // Assert
        assertThat(result.userId()).isEqualTo(defaultUser.getId());
        assertThat(result.tenantId()).isEqualTo(defaultTenantId);
        assertThat(entity.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(entity);
    }

    @Test
    void validateAndRevokeRefreshToken_WithAlreadyRevokedToken_RevokesAllAndThrowsException() {
        // Arrange
        String refreshToken = jwtService.generateRefreshToken(defaultUser);
        String tokenHash = jwtService.hashToken(refreshToken);
        RefreshToken entity = RefreshToken.builder()
                .userId(defaultUser.getId())
                .tokenHash(tokenHash)
                .revoked(true)
                .build();

        when(tokenBlacklistService.isTokenRevoked(refreshToken)).thenReturn(false);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(entity));
        // ponytail: ejecutar la lambda de txTemplate para que deleteByUserId se llame y el verify pase
        doAnswer(invocation -> {
            var consumer = invocation.getArgument(0, java.util.function.Consumer.class);
            consumer.accept(null);
            return null;
        }).when(txTemplate).executeWithoutResult(any());

        // Act & Assert
        assertThatThrownBy(() -> jwtService.validateAndRevokeRefreshToken(refreshToken))
                .isInstanceOf(TokenRevokedException.class)
                .hasMessageContaining("REUSE DETECTED");

        verify(refreshTokenRepository).deleteByUserId(defaultUser.getId());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shortSecretThrowsException() throws Exception {
        var utils = new JwtServiceImpl(tokenBlacklistService, refreshTokenRepository, txTemplate);
        ReflectionTestUtils.setField(utils, "secretKey", "short");
        ReflectionTestUtils.setField(utils, "accessTokenExpiration", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(utils, "refreshTokenExpiration", REFRESH_EXPIRATION);
        assertThatThrownBy(utils::init).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveRefreshToken_SavesCorrectEntity() {
        // Act
        String refreshToken = "some-refresh-token";
        jwtService.saveRefreshToken(defaultUser, defaultTenantId, refreshToken);

        // Assert
        verify(refreshTokenRepository).save(argThat(entity ->
            entity.getUserId().equals(defaultUser.getId()) &&
            entity.getTenantId().equals(defaultTenantId) &&
            entity.getTokenHash().equals(jwtService.hashToken(refreshToken)) &&
            Boolean.FALSE.equals(entity.getRevoked())
        ));
    }
}
