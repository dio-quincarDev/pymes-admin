package auth.pymes.common.config;

import auth.pymes.common.models.dto.request.OAuth2IntentRequest;
import auth.pymes.common.models.entities.Tenant;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.common.models.enums.PlanName;
import auth.pymes.common.models.enums.RoleName;
import auth.pymes.repositories.TenantRepository;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.JwtService;
import auth.pymes.service.OAuth2IntentService;
import auth.pymes.utils.exception.custom.ResourceNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static auth.pymes.utils.exception.CodigoError.USER_NOT_FOUND_BY_EMAIL;

/**
 * Manejador de éxito tras autenticación OAuth2.
 * Transforma la sesión de Google/FB en un JWT propio de PyMes Admin.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserEntityRepository userRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRepository tenantRepository;
    private final OAuth2IntentService oauth2IntentService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cors.allowed-origins:http://localhost:9200}")
    private String frontendUrl;

    private static final Duration CODE_TTL = Duration.ofMinutes(2);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String intentId = extractIntentIdFromCookie(request);

        log.info("OAuth2 Login exitoso para: {}. intentId: {}", email, intentId);

        // 1. Buscar usuario en DB
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND_BY_EMAIL, email));

        UUID activeTenantId = null;
        String role = "USER";
        String plan = "FREE";

        // 2. Prioridad 1: Si hay un intent, crear la nueva empresa (el usuario lo pidió explícitamente)
        if (StringUtils.hasText(intentId)) {
            Optional<OAuth2IntentRequest> intentOpt = oauth2IntentService.getIntent(intentId);
            if (intentOpt.isPresent()) {
                OAuth2IntentRequest intent = intentOpt.get();
                log.info("Procesando intent para empresa nueva: {}", intent.companyName());

                Tenant tenant = Tenant.builder()
                        .name(intent.companyName())
                        .slug(intent.companySlug())
                        .plan(PlanName.FREE)
                        .isActive(true)
                        .build();
                tenant = tenantRepository.save(tenant);

                UserTenant userTenant = UserTenant.builder()
                        .userId(user.getId())
                        .tenantId(tenant.getId())
                        .role(RoleName.OWNER)
                        .isActive(true)
                        .acceptedAt(ZonedDateTime.now())
                        .build();
                userTenantRepository.save(userTenant);

                activeTenantId = tenant.getId();
                role = RoleName.OWNER.name();
                plan = PlanName.FREE.name();

                oauth2IntentService.deleteIntent(intentId);
                clearIntentCookie(request, response);
            }
        }

        // 3. Prioridad 2: Si NO hubo intent o no era válido, buscar si ya tiene empresas existentes
        if (activeTenantId == null) {
            List<UserTenant> userTenants = userTenantRepository.findByUserId(user.getId());
            if (!userTenants.isEmpty()) {
                UserTenant ut = userTenants.get(0);
                activeTenantId = ut.getTenantId();
                role = ut.getRole().name();
                
                Tenant tenant = tenantRepository.findById(activeTenantId).orElse(null);
                if (tenant != null) {
                    plan = tenant.getPlan().name();
                }
                log.info("Usuario {} ya tiene tenant(s). Usando el existente: {} ({})", email, activeTenantId, role);
                
                // Limpiar cookie si existía pero no se usó porque el intent falló
                clearIntentCookie(request, response);
            }
        }

        // 5. Generar Tokens JWT
        String accessToken = jwtService.generateAccessToken(user, activeTenantId, role, plan);
        String refreshToken = jwtService.generateRefreshToken(user);
        jwtService.saveRefreshToken(user, activeTenantId, refreshToken);

        // 6. Guardar tokens en Redis con código de un solo uso
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("oauth:code:" + code, new HashMap<>(Map.of("accessToken", accessToken, "refreshToken", refreshToken)), CODE_TTL);

        // 7. Redirigir al frontend solo con el código (sin JWT en URL)
        String targetUrl = frontendUrl + "/#/auth/callback?code=" + code;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String extractIntentIdFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> "oauth2_intent".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(null);
    }

    private void clearIntentCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        Arrays.stream(cookies)
                .filter(c -> "oauth2_intent".equals(c.getName()))
                .findFirst()
                .ifPresent(c -> {
                    c.setValue("");
                    c.setMaxAge(0);
                    c.setPath("/");
                    response.addCookie(c);
                });
    }
}
