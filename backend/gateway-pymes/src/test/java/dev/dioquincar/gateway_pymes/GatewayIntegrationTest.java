package dev.dioquincar.gateway_pymes;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    private static final String TEST_SECRET =
            "988cf74e0e513f5f56af0f421f83c2169e0f49a0a780096469dfc4d4102cfab3_TEST_ONLY";
    private static final String TEST_ORIGIN = "https://app.test";

    private static final MockWebServer coreServer = startServer();
    private static final MockWebServer authServer = startServer();

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveStringRedisTemplate redisTemplate;

    private static MockWebServer startServer() {
        try {
            MockWebServer server = new MockWebServer();
            server.start();
            return server;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo iniciar MockWebServer", e);
        }
    }

    @AfterAll
    static void stopServers() throws Exception {
        coreServer.shutdown();
        authServer.shutdown();
    }

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("CORE_SERVICE_HOST", () -> "localhost");
        registry.add("CORE_SERVICE_PORT", () -> coreServer.getPort());
        registry.add("AUTH_SERVICE_HOST", () -> "localhost");
        registry.add("AUTH_SERVICE_PORT", () -> authServer.getPort());
        registry.add("CORS_ALLOWED_ORIGINS", () -> TEST_ORIGIN);
        registry.add("jwt.secret", () -> TEST_SECRET);
        registry.add("management.health.redis.enabled", () -> "false");
    }

    @BeforeEach
    void allowTokenNotRevoked() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
    }

    @Test
    @DisplayName("G-IT1: GET /api/v1/core/setup/config con JWT llega al upstream de core")
    void proxyRutaCoreProtegida() {
        enqueueJson(coreServer, "{\"ok\":true}");

        webTestClient.get().uri("/api/v1/core/setup/config")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.ok").isEqualTo(true);
    }

    @Test
    @DisplayName("G-IT2: POST /api/v1/auth/login es público y llega al upstream de auth")
    void proxyRutaAuthPublica() {
        enqueueJson(authServer, "{\"token\":\"abc\"}");

        webTestClient.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.token").isEqualTo("abc");
    }

    @Test
    @DisplayName("G-IT3: /api/v1/core/** matchea la ruta de core y NO la genérica de auth")
    void ordenRutasCoreNoCaeEnAuth() {
        int authRequestsBefore = authServer.getRequestCount();
        enqueueJson(coreServer, "{\"items\":[]}");

        webTestClient.get().uri("/api/v1/core/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .exchange()
                .expectStatus().isOk();

        assertThat(authServer.getRequestCount()).isEqualTo(authRequestsBefore);
    }

    @Test
    @DisplayName("G-IT4: preflight OPTIONS devuelve CORS de origen exacto con credenciales")
    void corsPreflightPermiteOrigenConfigurado() {
        webTestClient.options().uri("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, TEST_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TEST_ORIGIN)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        "GET,POST,PUT,PATCH,DELETE,OPTIONS");
    }

    @Test
    @DisplayName("G-IT4b: preflight OPTIONS con origen no permitido devuelve 403")
    void corsPreflightOrigenNoPermitidoDevuelve403() {
        webTestClient.options().uri("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://evil.test")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("G-IT5: respuestas del gateway incluyen security headers")
    void securityHeadersPresentes() {
        enqueueJson(authServer, "{\"ok\":true}");

        webTestClient.get().uri("/api/v1/auth/login")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Strict-Transport-Security",
                        "max-age=31536000; includeSubDomains")
                .expectHeader().valueEquals("X-Frame-Options", "DENY")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff");
    }

    @Test
    @DisplayName("G-IT6: ruta protegida sin Authorization devuelve 401 JSON")
    void jwtAusenteDevuelve401() {
        webTestClient.get().uri("/api/v1/core/setup/config")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.error").isEqualTo("No authorization header")
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    @DisplayName("G-IT7: JWT expirado devuelve 401 JSON")
    void jwtExpiradoDevuelve401() {
        webTestClient.get().uri("/api/v1/core/setup/config")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredAccessToken())
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Invalid JWT token")
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    @DisplayName("G-IT8: JWT válido forwarda X-User-Id, X-Tenant-Id y rol al upstream")
    void forwardIdentityHeadersAlUpstream() throws Exception {
        // Descarta requests de tests previos: getRequestCount() es acumulativo, la cola sí se vacía.
        while (coreServer.takeRequest(1, TimeUnit.MILLISECONDS) != null) {
            // drain
        }
        enqueueJson(coreServer, "{\"ok\":true}");
        String userId = UUID.randomUUID().toString();
        String tenantId = UUID.randomUUID().toString();

        webTestClient.get().uri("/api/v1/core/setup/config")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(userId, tenantId))
                .exchange()
                .expectStatus().isOk();

        RecordedRequest upstream = coreServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(upstream).isNotNull();
        assertThat(upstream.getHeader("X-User-Id")).isEqualTo(userId);
        assertThat(upstream.getHeader("X-Tenant-Id")).isEqualTo(tenantId);
        assertThat(upstream.getHeader("X-User-Email")).isEqualTo("owner@pymes.test");
        assertThat(upstream.getHeader("X-User-Role")).isEqualTo("OWNER");
        assertThat(upstream.getHeader("X-User-Plan")).isEqualTo("FREE");
    }

    @Test
    @DisplayName("G-IT9: header X-User-Role spoofeado por el cliente no sobrescribe el del JWT")
    void headerSpoofeadoNoSobrescribeJwt() throws Exception {
        while (coreServer.takeRequest(1, TimeUnit.MILLISECONDS) != null) { }
        enqueueJson(coreServer, "{\"ok\":true}");
        String userId = UUID.randomUUID().toString();
        String tenantId = UUID.randomUUID().toString();

        webTestClient.get().uri("/api/v1/core/setup/config")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(userId, tenantId))
                .header("X-User-Role", "ADMIN") // intento de spoofing
                .exchange()
                .expectStatus().isOk();

        RecordedRequest upstream = coreServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(upstream).isNotNull();
        assertThat(upstream.getHeader("X-User-Role")).isEqualTo("OWNER");
    }

    @Test
    @DisplayName("Actuator: /actuator/health es público y responde UP")
    void actuatorHealthPublico() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    private static void enqueueJson(MockWebServer server, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(body));
    }

    private static String accessToken() {
        return accessToken(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    private static String accessToken(String userId, String tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("owner@pymes.test")
                .claims(Map.of(
                        "userId", userId,
                        "tenantId", tenantId,
                        "role", "OWNER",
                        "plan", "FREE"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(secretKey())
                .compact();
    }

    private static String expiredAccessToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("owner@pymes.test")
                .claims(Map.of("role", "OWNER"))
                .issuedAt(Date.from(now.minusSeconds(600)))
                .expiration(Date.from(now.minusSeconds(300)))
                .signWith(secretKey())
                .compact();
    }

    private static SecretKey secretKey() {
        return Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
