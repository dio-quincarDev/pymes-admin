package auth.pymes.common.config;

import auth.pymes.common.models.dto.response.ErrorResponse;
import auth.pymes.common.models.entities.UserEntity;
import auth.pymes.repositories.UserEntityRepository;
import auth.pymes.service.JwtService;
import auth.pymes.utils.exception.auth.AuthApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtro de seguridad JWT para PyMes Admin.
 * Valida el token en cada petición y establece el contexto Multi-tenant.
 * Delega toda la validación del token a {@link JwtService#validateToken(String)}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserEntityRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return Arrays.stream(SecurityConfig.WHITE_LIST)
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

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
            // 2. Validar token y extraer claims (única fuente de verdad)
            JwtService.ValidatedToken validated = jwtService.validateToken(jwt);

            if (validated.userId() != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 3. Validación obligatoria en DB (Usuario debe existir y estar activo)
                UserEntity user = userRepository.findById(validated.userId()).orElse(null);

                if (user != null && user.isEnabled()) {

                    // 4. Crear autoridades (roles) dinámicas basadas en el JWT
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + validated.role())
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 5. Inyectar el Tenant ID en los atributos de la petición para uso posterior
                    request.setAttribute("X-Tenant-Id", validated.tenantId());

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario {} autenticado exitosamente para el Tenant {}", user.getEmail(), validated.tenantId());
                }
            }
        } catch (AuthApiException e) {
            log.error("Error de autenticación JWT [{}]: {}", e.getCodigo(), e.getMessage());
            sendErrorResponse(response, request, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request,
                                   AuthApiException ex) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(ex.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCodigo(),
                ex.getCodigoError().getMensaje(),
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
