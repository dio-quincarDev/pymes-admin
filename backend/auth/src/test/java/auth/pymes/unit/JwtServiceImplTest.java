package auth.pymes.unit;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.service.impl.JwtServiceImpl;
import auth.pymes.service.impl.TokenBlacklistService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION = 3600000L;
    private static final long REFRESH_EXPIRATION = 86400000L;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

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
}
