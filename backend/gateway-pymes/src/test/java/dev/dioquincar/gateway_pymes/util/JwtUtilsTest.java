package dev.dioquincar.gateway_pymes.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private static final String SECRET = "988cf74e0e513f5f56af0f421f83c2169e0f49a0a780096469dfc4d4102cfab3";
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();
        var field = JwtUtils.class.getDeclaredField("secret");
        field.setAccessible(true);
        field.set(jwtUtils, SECRET);
        jwtUtils.init();
    }

    @Test
    void validToken() {
        String token = createToken(SECRET, "user@test.com", 3600000);
        var claims = jwtUtils.getClaims(token);
        assertEquals("user@test.com", claims.getSubject());
    }

    @Test
    void expiredToken() {
        String token = createToken(SECRET, "user@test.com", -1000);
        assertThrows(Exception.class, () -> jwtUtils.getClaims(token));
    }

    @Test
    void invalidSignature() {
        String token = createToken("other-secret-that-is-different-256-bit-key-for-test", "user@test.com", 3600000);
        assertThrows(Exception.class, () -> jwtUtils.getClaims(token));
    }

    @Test
    void malformedToken() {
        assertThrows(Exception.class, () -> jwtUtils.getClaims("not-a-jwt"));
    }

    @Test
    void shortSecretThrowsException() throws Exception {
        var utils = new JwtUtils();
        var field = JwtUtils.class.getDeclaredField("secret");
        field.setAccessible(true);
        field.set(utils, "short");
        assertThrows(IllegalArgumentException.class, utils::init);
    }

    private static String createToken(String secret, String subject, long ttlMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs))
                .signWith(key)
                .compact();
    }
}
