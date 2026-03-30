package auth.pymes.common.config;

import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filtro de seguridad JWT para PyMes Admin.
 * Valida el token en cada petición y establece el contexto Multi-tenant.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserEntityRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Verificar presencia de Token Bearer
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            // 2. Extraer Identidad y validar estructura/expiración
            UUID userId = jwtService.extractUserId(jwt);
            UUID tenantId = jwtService.extractTenantId(jwt);
            String role = jwtService.extractRole(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 3. Validación obligatoria en DB (Usuario debe existir y estar activo)
                UserEntity user = userRepository.findById(userId).orElse(null);

                if (user != null && user.isEnabled() && jwtService.isTokenValid(jwt)) {
                    
                    // 4. Crear autoridades (roles) dinámicas basadas en el JWT
                    // Usamos "ROLE_" + role para ser compatibles con @PreAuthorize
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. Inyectar el Tenant ID en los atributos de la petición para uso posterior
                    request.setAttribute("X-Tenant-Id", tenantId);
                    
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario {} autenticado exitosamente para el Tenant {}", user.getEmail(), tenantId);
                }
            }
        } catch (Exception e) {
            log.error("Error al procesar la autenticación JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
