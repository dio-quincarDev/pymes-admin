package auth.pymes.common.config;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.common.models.entities.UserTenant;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.repositories.UserTenantRepository;
import auth.pymes.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

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

    @Value("${app.cors.allowed-origins}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        log.info("OAuth2 Login exitoso para: {}", email);

        // 1. Buscar usuario en DB
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado tras OAuth2"));

        // 2. Buscar sus empresas (Tenants)
        List<UserTenant> userTenants = userTenantRepository.findByUserId(user.getId());
        
        UUID activeTenantId = null;
        String role = "USER"; // Rol por defecto si no tiene empresa

        if (!userTenants.isEmpty()) {
            // Por simplicidad, tomamos la primera empresa asociada
            UserTenant ut = userTenants.get(0);
            activeTenantId = ut.getTenantId();
            role = ut.getRole().name();
        }

        // 3. Generar Tokens JWT usando el nuevo JwtService
        String accessToken = jwtService.generateAccessToken(user, activeTenantId, role);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 4. Construir URL de redirección al Frontend
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/callback")
                .queryParam("token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
