package auth.pymes.service.impl;

import auth.pymes.common.models.entities.RefreshToken;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.RefreshTokenRepository;
import auth.pymes.service.JwtService;
import auth.pymes.utils.exception.auth.AuthApiException;
import auth.pymes.utils.exception.token.TokenExpiredException;
import auth.pymes.utils.exception.token.TokenInvalidException;
import auth.pymes.utils.exception.token.TokenRevokedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Implementación oficial del motor de seguridad JWT.
 * Basado en JJWT 0.12.6, con soporte para Multi-tenancy y revocación (Redis + DB).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public String generateAccessToken(UserEntity user, UUID tenantId, String role, String plan) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tenantId", tenantId != null ? tenantId.toString() : null);
        claims.put("role", role);
        claims.put("plan", plan != null ? plan : "FREE");

        log.debug("Generando Access Token para usuario: {}, Tenant: {}, Plan: {}", user.getEmail(), tenantId, plan);
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
                .id(UUID.randomUUID().toString())
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
    public ValidatedToken validateToken(String token) throws AuthApiException {
        try {
            Claims claims = extractAllClaims(token);

            if (isTokenRevoked(token)) {
                throw new TokenRevokedException();
            }

            if (isTokenExpired(token)) {
                throw new TokenExpiredException();
            }

            String userIdStr = claims.get("userId", String.class);
            String tenantIdStr = claims.get("tenantId", String.class);
            String role = claims.get("role", String.class);
            String email = claims.getSubject();

            UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
            UUID tenantId = tenantIdStr != null ? UUID.fromString(tenantIdStr) : null;

            return new ValidatedToken(userId, tenantId, role, email);

        } catch (ExpiredJwtException e) {
            log.error("Token JWT expirado: {}", e.getMessage());
            throw new TokenExpiredException();
        } catch (SignatureException e) {
            log.error("Firma JWT inválida: {}", e.getMessage());
            throw new TokenInvalidException();
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformado: {}", e.getMessage());
            throw new TokenInvalidException();
        } catch (IllegalArgumentException e) {
            log.error("Token JWT con argumento ilegal: {}", e.getMessage());
            throw new TokenInvalidException();
        } catch (TokenRevokedException | TokenExpiredException | TokenInvalidException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al validar token JWT: {}", e.getMessage());
            throw new TokenInvalidException();
        }
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
    public String extractPlan(String token) {
        String plan = extractAllClaims(token).get("plan", String.class);
        return plan != null ? plan : "FREE";
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
        // Añadir el token a la blacklist hasta que expire naturalmente
        tokenBlacklistService.revokeToken(token, accessTokenExpiration);
        log.info("Token revocado y añadido a blacklist");
    }

    @Override
    public boolean isTokenRevoked(String token) {
        return tokenBlacklistService.isTokenRevoked(token);
    }

    @Override
    @Transactional
    public RefreshTokenValidation validateAndRevokeRefreshToken(String refreshToken) throws AuthApiException {
        if (!isTokenValid(refreshToken)) {
            throw new TokenExpiredException("Refresh token has expired or is invalid");
        }

        String tokenHash = hashToken(refreshToken);
        RefreshToken entity = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenInvalidException("Refresh token not found"));

        if (Boolean.TRUE.equals(entity.getRevoked())) {
            log.error("¡REUSO DETECTADO para el usuario {}! Revocando toda la familia de tokens.", entity.getUserId());
            // Estrategia de seguridad: revocar todos los tokens del usuario si hay reuso sospechoso
            refreshTokenRepository.deleteByUserId(entity.getUserId());
            throw new TokenRevokedException("Refresh token has already been used (REUSE DETECTED)");
        }

        // Marcar el token actual como usado (rotación exitosa)
        entity.setRevoked(true);
        refreshTokenRepository.save(entity);

        return new RefreshTokenValidation(entity.getUserId(), entity.getTenantId());
    }

    @Override
    @Transactional
    public void saveRefreshToken(UserEntity user, UUID tenantId, String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        ZonedDateTime expiresAt = ZonedDateTime.now().plus(refreshTokenExpiration, java.time.temporal.ChronoUnit.MILLIS);
        RefreshToken entity = RefreshToken.builder()
                .userId(user.getId())
                .tenantId(tenantId)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        log.debug("Refresh token guardado exitosamente para el usuario={}", user.getEmail());
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

    @Override
    public String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
