package dev.dioquincar.gateway_pymes.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.error("JWT secret must be at least 256 bits (32 bytes), got {} bits", keyBytes.length * 8);
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isInvalid(String token) {
        try {
            getClaims(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
