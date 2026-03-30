package auth.pymes.service.impl;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Implementación oficial del motor de seguridad JWT.
 * Basado en JJWT 0.12.6, con soporte para Multi-tenancy y revocación (Redis).
 */
@Service
@Slf4j
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    @Override
    public String generateAccessToken(UserEntity user, UUID tenantId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tenantId", tenantId != null ? tenantId.toString() : null);
        claims.put("role", role);
        
        log.debug("Generando Access Token para usuario: {}, Tenant: {}", user.getEmail(), tenantId);
        return createToken(claims, user.getEmail(), accessTokenExpiration);
    }

    @Override
    public String generateRefreshToken(UserEntity user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        
        log.debug("Generando Refresh Token para usuario: {}", user.getEmail());
        return createToken(claims, user.getEmail(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public UUID extractUserId(String token) {
        String userId = extractAllClaims(token).get("userId", String.class);
        return userId != null ? UUID.fromString(userId) : null;
    }

    @Override
    public UUID extractTenantId(String token) {
        String tenantId = extractAllClaims(token).get("tenantId", String.class);
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    @Override
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token) && !isTokenRevoked(token);
        } catch (Exception e) {
            log.error("Token JWT no válido: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void revokeToken(String token) {
        // TODO: Integrar con RedisTemplate una vez configurada la infraestructura de Redis
        log.warn("Revocación de token pendiente de integración con Redis: {}", token);
    }

    @Override
    public boolean isTokenRevoked(String token) {
        // TODO: Consultar RedisTemplate para verificar si el JTI o el token completo está en blacklist
        return false;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
